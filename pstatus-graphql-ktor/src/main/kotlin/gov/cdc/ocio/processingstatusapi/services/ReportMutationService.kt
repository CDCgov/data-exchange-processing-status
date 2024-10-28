package gov.cdc.ocio.processingstatusapi.services

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
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
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*


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
    fun upsertReport(action: String, input: ReportInput): Report? {
        logger.info("ReportId, id = ${input.id}, action = $action")

        return try {
            performUpsert(input, action)
        } catch (e: BadRequestException) {
            logger.error("Bad request while upserting report: ${e.message}", e)
            throw e // Re-throwing the BadRequestException
        } catch (e: ContentException) {
            logger.error("Content error while upserting report: ${e.message}", e)
            throw e // Re-throwing the ContentException
        } catch (e: Exception) {
            logger.error("Unexpected error while upserting report: ${e.message}", e)
            throw ContentException("An unexpected error occurred: ${e.message}") // Re-throwing the Exception
        }
    }

    /**
     * Executes the upsert operation for a report.
     *
     * Validates the action type and performs either a create or replace operation on the report.
     *
     *  * Additionally, checks for the presence of required fields: `dataStreamId`, `dataStreamRoute`,
     *  * and both `stageInfo.service` and `stageInfo.action`. Throws a BadRequestException if any of
     *  * these fields are missing.
     *
     * Generates a new ID for the report if creating, and checks for existence when replacing.
     *
     * @param input The ReportInput containing details of the report to be created or replaced.
     * @param action A string specifying the action to perform: "create" or "replace".
     * @return The updated or newly created Report, or null if the operation fails.
     * @throws BadRequestException If the action is invalid or the ID is improperly provided.
     */
    @Throws(BadRequestException::class, ContentException::class)
    private fun performUpsert(input: ReportInput, action: String): Report? {
        // Validate action parameter
        val upsertAction = UpsertAction.fromString(action)

        // Validate required fields for ReportInput
        validateInput(input, upsertAction)

        return try {
            val report = mapInputToReport(input)

            when (upsertAction) {
                UpsertAction.CREATE -> createReport(report)
                UpsertAction.REPLACE -> replaceReport(report)
            }
        } catch (e: BadRequestException) {
            logger.error("Validation error during upsert: ${e.message}", e)
            throw e // Re-throwing the BadRequestException
        } catch (e: ContentException) {
            logger.error("Content error during upsert: ${e.message}", e)
            throw e // Re-throwing the ContentException
        } catch (e: Exception) {
            logger.error("Unexpected error during upsert: ${e.message}", e)
            throw ContentException("Failed to perform upsert: ${e.message}")
        }
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

        return try {

            // Parse the content based on its type
            val parsedContent = input.content?.let { parseContent(it, input.contentType) } as? Map<*, *>?

            // Set id and reportId to be the same
            val reportId = input.id // Generate a new ID if not provided


            Report(
                id = reportId,
                uploadId = input.uploadId,
                reportId = reportId, //Set reportId to be the same as id
                dataStreamId = input.dataStreamId,
                dataStreamRoute = input.dataStreamRoute,
                dexIngestDateTime = input.dexIngestDateTime,
                messageMetadata = input.messageMetadata?.let { mapInputToMessageMetadata(it) },
                stageInfo = input.stageInfo?.let { mapInputToStageInfo(it) },
                tags = input.tags?.associate { it.key to it.value },
                data = input.data?.associate { it.key to it.value },
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
     * Supports JSON and Base64 content types, converting them into a Map structure.
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
            ReportContentType.BASE64 -> {
                try {
                    // Decode base64 content into a Map, if expected
                    val decodedBytes = Base64.getDecoder().decode(content)
                    val decodedString = String(decodedBytes)
                    // If the decoded base64 string is in JSON format, parse it
                    objectMapper.readValue<Map<String, Any>>(decodedString)
                } catch (e: IllegalArgumentException) {
                    logger.error("Invalid Base64 string: ${e.message}")
                    throw ContentException("Invalid Base64 format: ${e.message}")
                } catch (e: JsonProcessingException) {
                    logger.error("Invalid JSON format after base64 decode: ${e.message}")
                    throw ContentException("Invalid JSON format after base64 decode: ${e.message}")
                }
            }
            else -> {
                throw ContentException("Unsupported content type: $contentType")
            }
        }
    }


    /**
     * Validates the input for a report based on the specified action.
     *
     * This function checks the validity of the provided `input` object based on the
     * specified `action` (CREATE or REPLACE). It ensures that the required fields
     * are present and valid. Specifically, for the CREATE action, it checks that
     * no ID is provided, while for the REPLACE action, it verifies that an ID is
     * supplied. Additionally, it validates the presence of `dataStreamId`,
     * `dataStreamRoute`, and checks the properties of `stageInfo`.
     *
     * @param input The ReportInput object to be validated. It must contain the necessary
     *              fields based on the action specified.
     * @param action The UpsertAction indicating the type of operation (CREATE or REPLACE).
     * @throws BadRequestException If the input is invalid, such as:
     *         - For CREATE: ID is provided.
     *         - For REPLACE: ID is missing.
     *         - If any of the required fields are missing: dataStreamId, dataStreamRoute,
     *           or stageInfo (including service and action).
     */
    @Throws (BadRequestException::class)
    private fun validateInput(input: ReportInput, action: UpsertAction) {
        when (action) {
            UpsertAction.CREATE -> {
                if (input.id != null) {
                    throw BadRequestException("ID should not be provided for create action.Provided ID: ${input.id}")
                }
            }
            UpsertAction.REPLACE -> {
                if (input.id.isNullOrBlank()) {
                    throw BadRequestException("ID must be provided for replace action.")
                }
                // Ensure reportId matches id if both are provided
                if (!input.reportId.isNullOrBlank() && input.id != input.reportId) {
                    throw BadRequestException("ID and reportId must be the same for replace action.")
                }
            }
        }

        // Validate dataStreamId and dataStreamRoute fields
        if (input.dataStreamId.isNullOrBlank() || input.dataStreamRoute.isNullOrBlank()) {
            throw BadRequestException("Missing required fields: dataStreamId and dataStreamRoute must be present.")
        }

        // Check if stageInfo is null
        if (input.stageInfo == null) {
            throw BadRequestException("Missing required field: stageInfo must be present.")
        }

        // Check properties of stageInfo
        if (input.stageInfo.service.isNullOrBlank() || input.stageInfo.action.isNullOrBlank()) {
            throw BadRequestException("Missing required fields in stageInfo: service and action must be present.")
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
    @Throws(BadRequestException::class, ContentException:: class)
    private fun createReport(report: Report): Report? {

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
    private fun replaceReport(report: Report): Report? {

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
                } catch(e: Exception) {
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

enum class UpsertAction(val action: String) {
    CREATE("create"),
    REPLACE("replace");

    companion object {
        fun fromString(action: String): UpsertAction {
            return entries.find { it.action.equals(action, ignoreCase = true) }
                ?: throw BadRequestException("Invalid action specified: $action. Must be 'create' or 'replace'.")
        }
    }
}
