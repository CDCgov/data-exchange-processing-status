package gov.cdc.ocio.processingstatusapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.database.models.Report
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.database.utils.DateLongFormatTypeAdapter
import gov.cdc.ocio.database.utils.InstantTypeAdapter
import gov.cdc.ocio.database.utils.OffsetDateTimeTypeAdapter
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.extensions.snakeToCamelCase
import gov.cdc.ocio.processingstatusapi.collections.BasicHashMap
import gov.cdc.ocio.processingstatusapi.services.ValidationComponents.gson
import gov.cdc.ocio.reportschemavalidator.errors.ErrorLoggerProcessor
import gov.cdc.ocio.reportschemavalidator.exceptions.ValidationException
import gov.cdc.ocio.reportschemavalidator.loaders.FileSchemaLoader
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.reportschemavalidator.validators.JsonSchemaValidator
import mu.KLogger
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*


object ValidationComponents {
    private val objectMapper: ObjectMapper by lazy { ObjectMapper() }
    val jsonUtils: DefaultJsonUtils by lazy { DefaultJsonUtils(objectMapper) }
    val schemaLoader: FileSchemaLoader by lazy { FileSchemaLoader() }
    val schemaValidator: JsonSchemaValidator by lazy { JsonSchemaValidator(logger) }
    val errorProcessor: ErrorLoggerProcessor by lazy { ErrorLoggerProcessor(logger) }
    val logger: KLogger by lazy { KotlinLogging.logger {} }

    val gson: Gson by lazy {
        GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(Date::class.java, DateLongFormatTypeAdapter())
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .serializeNulls()
            .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeTypeAdapter())
            .create()
    }
}

/**
 * ReportMutation class handles the creation and replacement of reports in a Cosmos DB.
 *
 * This class extends the CosmosLoader and provides methods to upsert reports based on specified actions.
 *
 * Key functionalities include:
 * - `upsertReport`: A public method to create or replace a report, validating the input action.
 * - `performUpsert`: A private method that contains the core logic for creating or replacing reports.
 * - `mapInputToReport`: Transforms a ReportInput object into a Report object.
 * - Various mapping methods to convert input data into the appropriate report structures, including
 *   message metadata, stage info, and issues.
 * - `parseContent`: A method to handle different content types (JSON and Base64) and convert them
 *   into usable formats.
 *
 */
class ReportMutationService : KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val reportsCollection = repository.reportsCollection

    /**
     * Upserts a report based on the provided input and action.
     *
     * This method either creates a new report or replaces an existing one based on the specified action.
     * It validates the input and generates a new ID if the action is "create" and no ID is provided.
     * If the action is "replace", it ensures that the report ID is provided and that the report exists.
     *
     * @param input The ReportInput containing details of the report to be created or replaced.
     * @param action A string specifying the action to perform: "create" or "replace".
     * @return The updated or newly created Report, or null if the operation fails.
     * @throws BadRequestException If the action is invalid or if the ID is improperly provided.
     * @throws ContentException If there is an error with the content format.
     */
    @Throws(BadRequestException::class, ContentException::class, Exception::class)
    fun upsertReport(action: String, input: BasicHashMap<String, Any?>): Map<String, Any> {
        val result = runCatching {
            // Convert to a standard hash map
            val mapOfContent = input.toHashMap()

            // Validate action
            val actionType = validateAction(action)

            // Validate the report
            val validatedReport = validateReport(mapOfContent)

            // Assign the report ids
            val reportId = generateNewId()
            validatedReport.id = reportId
            validatedReport.reportId = reportId

            when (actionType) {
                Action.CREATE -> createReport(validatedReport)
                Action.REPLACE -> replaceReport(validatedReport)
            }

            return mapOf(
                "result" to "SUCCESS",
                "uploadId" to (validatedReport.uploadId ?: "unknown"),
                "reportId" to (validatedReport.reportId ?: "unknown")
            )
        }

        when (val exception = result.exceptionOrNull()) {
            is ValidationException -> {
                return mapOf(
                    "result" to "FAILURE",
                    "reason" to exception.localizedMessage,
                    "issues" to exception.issues,
                    "schemaFileNames" to exception.schemaFileNames
                )
            }

            else -> {
                return mapOf(
                    "result" to "FAILURE",
                    "reason" to (result.exceptionOrNull()?.message ?: "unknown")
                )
            }
        }
    }

    private fun validateReport(input: Map<String, Any?>?): Report {
        try {
            logger.info("The report received: $input and will be converted to Json")
            val snakeCaseKeyReportJson = gson.toJson(input)

            logger.info("The report after converting to a Json: $snakeCaseKeyReportJson, report will be validated next")
            val schemaValidationService = SchemaValidationService(
                ValidationComponents.schemaLoader,
                ValidationComponents.schemaValidator,
                ValidationComponents.errorProcessor,
                ValidationComponents.jsonUtils,
                logger
            )
            val validationResult = schemaValidationService.validateJsonSchema(snakeCaseKeyReportJson)
            logger.info("status is $validationResult")

            // if status is successful, will persist report to Reports container, otherwise to dlq container
            if (validationResult.status) {
                // The report input comes in from graphql as snake case, but all the models are set up for camel case.
                val camelCaseKeyMap = mapKeysToCamelCase(input)
                val reportJson = gson.toJson(camelCaseKeyMap)
                val report = gson.fromJson(reportJson, Report::class.java)
                return report
            } else {
                throw ValidationException(
                    issues = validationResult.invalidData,
                    schemaFileNames = validationResult.schemaFileNames
                )
            }
        } catch (e: Exception) {
            logger.error("Exception occurred during validation $e.message")
            throw e
        }
    }

    /**
     * Re-map the keys of the provided map from snake case to camel case.
     *
     * @param map Map<String, Any?>?
     * @return Map<String, Any?>
     */
    private fun mapKeysToCamelCase(map: Map<String, Any?>?): Map<String, Any?> {
        val newMap = mutableMapOf<String, Any?>()

        if (map != null) {
            for ((key, value) in map) {
                val newKey = key.snakeToCamelCase() // Example transformation, adjust as needed

                val newValue = when (value) {
                    is Map<*, *> -> mapKeysToCamelCase(value as Map<String, Any?>) // Recursively convert nested maps
                    else -> value
                }

                newMap[newKey] = newValue
            }
        }

        return newMap
    }

    /**
     * Creates a new report in the database.
     *
     * This function generates a new ID for the report, validates the provided upload ID,
     * and attempts to create the report in the Cosmos DB container. If the report ID
     * is provided or if the upload ID is missing, a BadRequestException is thrown.
     * If there is an error during the creation process, a ContentException is thrown.
     *
     * @param report The report object to be created. Must not have an existing ID and must include a valid upload ID.
     * @return The created Report object, or null if the creation fails.
     * @throws BadRequestException If the report ID is provided or if the upload ID is missing.
     * @throws ContentException If there is an error during the report creation process.
     */
    @Throws(BadRequestException::class, ContentException::class)
    private fun createReport(report: Report) { // TODO - move to ReportManager.kt
        logger.info("Creating report for uploadId = ${report.uploadId} with stageName = ${report.stageInfo?.action}")
        val transformedContent = repository.contentTransformer(report.content as Map<*, *>)
        report.content = transformedContent

        try {
            val success =
                reportsCollection.createItem(
                    generateNewId(),
                    report,
                    Report::class.java,
                    report.uploadId
                )

            // Check if successful and throw an exception if not
            if (!success)
                throw ContentException("Failed to create report")

        } catch (e: Exception) {
            logger.error(e.localizedMessage)
            throw ContentException("Failed to create report: ${e.message}")
        }
    }

    /**
     * Replaces an existing report in the database.
     *
     * This function checks if the report ID is provided and validates the upload ID.
     * It attempts to read the existing report from the Cosmos DB container and,
     * if found, replaces it with the new report data. If the report ID is missing,
     * or if the upload ID is not provided, a BadRequestException is thrown. If
     * the report is not found for replacement, another BadRequestException is thrown.
     * In case of any error during the database operations, an appropriate exception
     * will be thrown.
     *
     * @param report The report object containing the new data. Must have a valid ID and upload ID.
     * @return The updated Report object, or null if the replacement fails.
     * @throws BadRequestException If the report ID is missing, the upload ID is missing, or the report is not found.
     */
    @Throws(BadRequestException::class, ContentException::class)
    private fun replaceReport(report: Report) { //TODO- move to ReportManager.kt and update
        return try {
            val uploadId = report.uploadId
            val stageInfo = report.stageInfo

            // Delete all reports matching the report ID with the same service and action name
            val cName = repository.reportsCollection.collectionNameForQuery
            val cVar = repository.reportsCollection.collectionVariable
            val cPrefix = repository.reportsCollection.collectionVariablePrefix
            val cElFunc = repository.reportsCollection.collectionElementForQuery
            val sqlQuery = (
                    "select * from $cName $cVar "
                            + "where ${cPrefix}uploadId = '$uploadId' "
                            + "and ${cPrefix}stageInfo.${cElFunc("service")} = '${stageInfo?.service}' "
                            + "and ${cPrefix}stageInfo.${cElFunc("action")} = '${stageInfo?.action}'"
                    )
            val items = repository.reportsCollection.queryItems(
                sqlQuery,
                Report::class.java
            )
            if (items.isNotEmpty()) {
                try {
                    items.forEach {
                        reportsCollection.deleteItem(
                            it.id,
                            it.uploadId
                        )
                    }
                    logger.info("Removed all reports with stage name = $stageInfo?.stage")
                } catch (e: Exception) {
                    throw BadStateException("Issue deleting report: ${e.localizedMessage}")
                }
            }

            createReport(report)

        } catch (e: Exception) {
            logger.error(e.localizedMessage)
            throw ContentException("Failed to replace report: ${e.message}")
        }
    }

    /**
     * Generates a new unique identifier for a report.
     *
     * This method creates a UUID string to be used as a unique ID when creating a new report.
     *
     * @return A unique string identifier.
     */
    private fun generateNewId(): String {
        // Generate a new unique ID
        return UUID.randomUUID().toString()
    }

    enum class Action {
        CREATE, REPLACE
    }

    private fun validateAction(action: String): Action {
        return when (action) {
            "create" -> {
                Action.CREATE
            }

            "replace" -> {
                Action.REPLACE
            }

            else -> throw BadRequestException("Invalid action provided: $action")
        }

    }
}
