package gov.cdc.ocio.processingstatusapi.mutations

import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.CosmosItemResponse
import com.azure.cosmos.models.PartitionKey
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.loaders.CosmosLoader
import gov.cdc.ocio.processingstatusapi.models.ReportContentType
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.reports.*
import gov.cdc.ocio.processingstatusapi.models.submission.Issue
import gov.cdc.ocio.processingstatusapi.models.submission.Level
import gov.cdc.ocio.processingstatusapi.models.submission.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.submission.StageInfo
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
class ReportMutation : CosmosLoader() {

    private val objectMapper = ObjectMapper()

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
    @Throws(BadRequestException::class)
    fun upsertReport(input: ReportInput, action: String): Report? {
        logger.info("ReportId, id = ${input.id}, action = $action")

        return try {
            performUpsert(input, action)
        } catch (e: Exception) {
            logger.error("Error upserting report: ${e.message}", e)
            throw BadRequestException("Failed to upsert report: ${e.message}")
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
    @Throws(BadRequestException::class)
    private fun performUpsert(input: ReportInput, action: String): Report? {
        // Validate action parameter
        val upsertAction = UpsertAction.fromString(action)

        // Validate required fields for ReportInput
        if (input.dataStreamId.isNullOrBlank() || input.dataStreamRoute.isNullOrBlank() ||
            input.stageInfo?.service.isNullOrBlank() || input.stageInfo?.action.isNullOrBlank()) {
            throw BadRequestException("Missing required fields: dataStreamId, dataStreamRoute, stageInfo.service, and stageInfo.action must be present.")
        }

        val report = mapInputToReport(input)

        return when (upsertAction) {
            UpsertAction.CREATE -> {
                if (!report.id.isNullOrBlank()) {
                    throw BadRequestException("ID should not be provided for create action.")
                }

                // Generate a new ID if not provided
                report.id = generateNewId()
                val options = CosmosItemRequestOptions()
                val createResponse: CosmosItemResponse<Report>? = reportsContainer?.createItem(report, PartitionKey(report.uploadId!!), options)
                createResponse?.item
            }
            UpsertAction.REPLACE -> {
                if (report.id.isNullOrBlank()) {
                    throw BadRequestException("ID must be provided for replace action.")
                }

                // Attempt to read the existing item
                val readResponse: CosmosItemResponse<Report>? = reportsContainer?.readItem(report.id!!, PartitionKey(report.uploadId!!), Report::class.java)

                if (readResponse != null) {
                    // Replace the existing item
                    val options = CosmosItemRequestOptions()
                    val replaceResponse: CosmosItemResponse<Report>? = reportsContainer?.replaceItem(report, report.id!!, PartitionKey(report.uploadId!!), options)
                    replaceResponse?.item
                } else {
                    throw BadRequestException("Report with ID ${report.id} not found for replacement.")
                }
            }
            else -> throw BadRequestException("Unexpected action: $action")
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
    private fun mapInputToReport(input: ReportInput): Report {
        // Parse the content based on its type
        val parsedContent = input.content?.let { parseContent(it, input.contentType) } as? Map<*, *>?

        return Report(
            id = input.id,
            uploadId = input.uploadId,
            reportId = input.reportId,
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
        return java.util.UUID.randomUUID().toString()
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
                } catch (e: Exception) {
                    throw ContentException("Invalid JSON format: ${e.message}")
                }
            }
            ReportContentType.BASE64 -> {
                // Decode base64 content into a Map, if expected
                val decodedBytes = Base64.getDecoder().decode(content)
                val decodedString = String(decodedBytes)
                // If the decoded base64 string is in JSON format, parse it
                try {
                    objectMapper.readValue<Map<String, Any>>(decodedString)
                } catch (e: Exception) {
                    throw ContentException("Invalid JSON format after base64 decode: ${e.message}")
                }
            }
            else -> {
                throw ContentException("Unsupported content type: $contentType")
            }
        }
    }
}

enum class UpsertAction(val action: String) {
    CREATE("create"),
    REPLACE("replace");

    companion object {
        fun fromString(action: String): UpsertAction {
            return values().find { it.action.equals(action, ignoreCase = true) }
                ?: throw BadRequestException("Invalid action specified: $action. Must be 'create' or 'replace'.")
        }
    }
}
