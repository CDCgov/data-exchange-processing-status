package gov.cdc.ocio.processingstatusapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.database.models.Report
import gov.cdc.ocio.database.utils.DateLongFormatTypeAdapter
import gov.cdc.ocio.database.utils.InstantTypeAdapter
import gov.cdc.ocio.database.utils.OffsetDateTimeTypeAdapter
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.extensions.snakeToCamelCase
import gov.cdc.ocio.processingstatusapi.collections.BasicHashMap
import gov.cdc.ocio.processingstatusapi.models.Action
import gov.cdc.ocio.processingstatusapi.mutations.models.UpsertReportResult
import gov.cdc.ocio.processingstatusapi.mutations.models.ValidatedReportResult
import gov.cdc.ocio.processingstatusapi.services.ValidationComponents.gson
import gov.cdc.ocio.reportschemavalidator.errors.ErrorLoggerProcessor
import gov.cdc.ocio.reportschemavalidator.exceptions.ValidationException
import gov.cdc.ocio.reportschemavalidator.loaders.SchemaLoader
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.reportschemavalidator.validators.JsonSchemaValidator
import io.ktor.server.application.*
import mu.KLogger
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*


/**
 * Object to warehouse the dependencies for the report schema validation service
 */
object ValidationComponents {
    private val objectMapper: ObjectMapper by lazy { ObjectMapper() }
    val jsonUtils: DefaultJsonUtils by lazy { DefaultJsonUtils(objectMapper) }
    val schemaValidator: JsonSchemaValidator by lazy { JsonSchemaValidator() }
    val errorProcessor: ErrorLoggerProcessor by lazy { ErrorLoggerProcessor() }
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
class ReportMutationService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val reportManager = ReportManager()

    private val schemaLoader by inject<SchemaLoader>()

    /**
     * Upsert a report based on the provided input and action.
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
    fun upsertReport(action: String, input: BasicHashMap<String, Any?>): UpsertReportResult {
        val result = runCatching {
            // Convert to a standard hash map
            val mapOfContent = input.toHashMap()

            // Validate action
            val actionType = validateAction(action)

            // Validate the report
            val validationResult = validateReport(mapOfContent)
            val validatedReport = validationResult.report!!

            // Assign the report ids
            val reportId = UUID.randomUUID().toString()
            validatedReport.id = reportId
            validatedReport.reportId = reportId

            when (actionType) {
                Action.CREATE -> reportManager.createReport(validatedReport)
                Action.REPLACE -> reportManager.replaceReport(validatedReport)
            }

            return UpsertReportResult(
                result = "SUCCESS",
                uploadId = validatedReport.uploadId ?: "unknown",
                reportId = validatedReport.reportId ?: "unknown",
                schemaFileNames = validationResult.validationSchemaResult?.schemaFileNames
            )
        }

        when (val exception = result.exceptionOrNull()) {
            is ValidationException -> {
                return UpsertReportResult(
                    result = "FAILURE",
                    reason  = exception.localizedMessage,
                    issues = exception.issues.toList(),
                    schemaFileNames = exception.schemaFileNames.toList()
                )
            }

            else -> {
                return UpsertReportResult(
                    result = "FAILURE",
                    reason = result.exceptionOrNull()?.message ?: "unknown"
                )
            }
        }
    }

    /**
     * Validates the report with the provided input [Map].
     *
     * @param input Map<String, Any?>?
     * @return ValidatedReportResult
     * @throws ContentException
     * @throws Exception
     */
    @Throws(ContentException::class, Exception::class)
    private fun validateReport(input: Map<String, Any?>?): ValidatedReportResult {

        if (input.isNullOrEmpty()) throw ContentException("Can't validate a null or empty report")

        try {
            logger.info("The report received: $input and will be converted to Json")
            val snakeCaseKeyReportJson = gson.toJson(input)

            logger.info("The report after converting to a Json: $snakeCaseKeyReportJson, report will be validated next")
            val schemaValidationService = SchemaValidationService(
                schemaLoader,
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
                return ValidatedReportResult(
                    validationSchemaResult = validationResult,
                    report = report
                )
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
     * Validates the provided action for the report disposition, which is to create or replace an existing report.
     *
     * @param action [String] - Action to be validated
     * @return [Action] - Validated action as an enumeration
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
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