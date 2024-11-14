package gov.cdc.ocio.processingstatusapi.services

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.database.utils.DateLongFormatTypeAdapter
import gov.cdc.ocio.database.utils.InstantTypeAdapter
import gov.cdc.ocio.database.utils.OffsetDateTimeTypeAdapter
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.ReportContentType
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.reports.*
import gov.cdc.ocio.processingstatusapi.models.reports.inputs.IssueInput
import gov.cdc.ocio.processingstatusapi.models.reports.inputs.MessageMetadataInput
import gov.cdc.ocio.processingstatusapi.models.reports.inputs.ReportInput
import gov.cdc.ocio.processingstatusapi.models.reports.inputs.StageInfoInput
import gov.cdc.ocio.processingstatusapi.models.submission.Issue
import gov.cdc.ocio.processingstatusapi.models.submission.Level
import gov.cdc.ocio.processingstatusapi.models.submission.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.submission.StageInfo
import gov.cdc.ocio.processingstatusapi.services.ValidationComponents.gson
import gov.cdc.ocio.reportschemavalidator.errors.ErrorLoggerProcessor
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

        private val objectMapper = ObjectMapper()

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
        fun upsertReport(action: String, input: ReportInput): Report {
            //validate action
            val actionType = validateAction(action)
            val report = mapInputToReport(input)
            when (actionType){
                Action.CREATE -> validateAndPersistReport(input)
                Action.REPLACE -> replaceReport(report)
            }
            return report
        }

        private fun validateAndPersistReport(input: ReportInput){
            try {
                //use Gson to convert object to Json
                logger.info("The report received: $input and will be converted to Json" )
                val report = gson.toJson(input)

                logger.info("The report after converting to a Json: $report, report will be validated next")
                val schemaValidationService = SchemaValidationService(
                    ValidationComponents.schemaLoader,
                    ValidationComponents.schemaValidator,
                    ValidationComponents.errorProcessor,
                    ValidationComponents.jsonUtils,
                    logger)
                val validationResult = schemaValidationService.validateJsonSchema(report)
                logger.info("status is $validationResult")
                // if status is successful, will persist report to Reports container, otherwise to dlq container
                if (validationResult.status) {
                    logger.info("Creating report for uploadId = ${input.uploadId} with stageName = ${input.stageInfo?.action}")

                    val reportObject = mapInputToReport(input)
                    createReport(reportObject)
                }else{
                    logger.info("The report failed to validate, persisting to dead-letter container")

                }
            }catch (e: Exception){
                logger.error("Exception occurred during validation$e.message")
            }

        }
        fun updateExistingReport(report: ReportInput){
            //place holder to add logic for replacing report
            logger.info("Updating existing report: $report")

        }

        /**
         * Maps the given ReportInput to a Report object.
         *
         * This method extracts the necessary fields from the input and constructs a Report instance.
         * It also parses the content based on its type.
         *
         * @param input The ReportInput containing the details to map to a Report.
         * @return A Report object populated with data from the input.
         */
        @Throws(ContentException::class)
        private fun mapInputToReport(input: ReportInput): Report {
            logger.info("TO DO - to be updated or removed" )
            return try {

               // Parse the content based on its type
                val parsedContent = input.content?.let { parseContent(it, input.contentType) } as? Map<*, *>?

                // Set id and reportId to be the same
                val reportId = input.id // Generate a new ID if not provided
                //val parsedContent = input.content?.let { parseContent(it, input.contentType) }

               Report(
                    id = reportId,
                    uploadId = input.uploadId,
                    reportId = reportId, //Set reportId to be the same as id
                    dataStreamId = input.dataStreamId,
                    dataStreamRoute = input.dataStreamRoute,
                    dexIngestDateTime = input.dexIngestDateTime,
                    messageMetadata = input.messageMetadata?.let { mapInputToMessageMetadata(it) },
                    stageInfo = input.stageInfo?.let { mapInputToStageInfo(it) },
                    tags = input.tags,
                    data = input.data,
                    contentType = input.contentType,
                    jurisdiction = input.jurisdiction,
                    senderId = input.senderId,
                    dataProducerId = input.dataProducerId,
                    content = parsedContent,
                    timestamp = input.timestamp
                )
            } catch (e: JsonProcessingException) {
                logger.error("JSON processing error mapping input to report: ${e.message}", e)
                throw ContentException("Failed to map input to report: ${e.message}")
            } catch (e: Exception) {
                logger.error("Error mapping input to report: ${e.message}", e)
                throw ContentException("Failed to map input to report: ${e.message}")
            }
        }

        /**
         * Maps the given MessageMetadataInput to a MessageMetadata object.
         *
         * Extracts fields from the input and creates a MessageMetadata instance.
         *
         * @param input The MessageMetadataInput to map.
         * @return A MessageMetadata object populated with data from the input.
         */
        private fun mapInputToMessageMetadata(input: MessageMetadataInput): MessageMetadata {
            return MessageMetadata(
                messageUUID = input.messageUUID,
                messageHash = input.messageHash,
                aggregation = input.aggregation,
                messageIndex = input.messageIndex
            )
        }

        /**
         * Maps the given StageInfoInput to a StageInfo object.
         *
         * Extracts fields from the input and creates a StageInfo instance, including issues.
         *
         * @param input The StageInfoInput to map.
         * @return A StageInfo object populated with data from the input.
         */
        private fun mapInputToStageInfo(input: StageInfoInput): StageInfo {
            return StageInfo(
                service = input.service,
                action = input.action,
                version = input.version,
                status = input.status,
                issues = input.issues?.map { mapInputToIssue(it) },
                startProcessingTime = input.startProcessingTime,
                endProcessingTime = input.endProcessingTime
            )
        }

        /**
         * Maps the given IssueInput to an Issue object.
         *
         * Extracts the level and message from the input and creates an Issue instance.
         *
         * @param input The IssueInput to map.
         * @return An Issue object populated with data from the input.
         */
        private fun mapInputToIssue(input: IssueInput): Issue {
            return Issue(
                level = input.code?.let { Level.valueOf(it) },
                message = input.description
            )
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

        /**
         * Parses the given content based on the specified content type.
         *
         * Supports JSON and assumes the rest is Base64 content type, converting them into a Map structure.
         *
         * @param content The content string to parse.
         * @param contentType The type of content (e.g., "application/json" or "base64").
         * @return A parsed representation of the content as a Map.
         * @throws ContentException If the content format is invalid or unsupported.
         */
        @Throws(ContentException::class)
        private fun parseContent(content: String, contentType: String?): Any {

            val validContentType = contentType?.let { ReportContentType.fromString(it) }

            return when (validContentType) {
                ReportContentType.JSON, ReportContentType.JSON_SHORT -> {
                    // Parse JSON content into a Map
                    try {
                        objectMapper.readValue<Map<String, Any>>(content)
                    } catch (e: JsonProcessingException) {
                        logger.error("Invalid JSON format: ${e.message}")
                        throw ContentException("Invalid JSON format: ${e.message}")
                    }
                }
                // if contentType is not JSON or JSON_SHORT, we can assume its base64 encoded string and return as is
                else-> content


            }
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
        private fun createReport(report: Report): Report? { // TODO - move to ReportManager.kt
            logger.info("createReport passed as $report")
            if (report.id != null) {
                throw BadRequestException("ID should not be provided for create action.")
            }

            // Validate uploadId
            if (report.uploadId.isNullOrBlank()) {
                throw BadRequestException("Upload ID must be provided.")
            }

            return try {
                val success =
                    reportsCollection.createItem(generateNewId(), report, Report::class.java, report.uploadId)

                // Check if successful and throw an exception if not
                if (!success)
                    throw ContentException("Failed to create report")

                report
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
        private fun replaceReport(report: Report): Report? { //TODO- move to ReportManager.kt and update

            if (report.id.isNullOrBlank()) {
                throw BadRequestException("ID must be provided for replace action.")
            }

            // Validate uploadId
            if (report.uploadId.isNullOrBlank()) {
                throw BadRequestException("Upload ID must be provided.")
            }

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
                    gov.cdc.ocio.database.models.Report::class.java
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

                val success = reportsCollection.createItem(
                    generateNewId(),
                    report,
                    Report::class.java,
                    uploadId
                )

                if (!success)
                    throw ContentException("Failed to replace report")

                report

            } catch (e: Exception) {
                logger.error(e.localizedMessage)
                throw ContentException("Failed to replace report: ${e.message}")
            }
        }

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

