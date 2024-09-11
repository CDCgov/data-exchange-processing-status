package gov.cdc.ocio.processingstatusapi.mutations

import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.CosmosItemResponse
import com.azure.cosmos.models.PartitionKey
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.loaders.CosmosLoader
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.reports.IssueInput
import gov.cdc.ocio.processingstatusapi.models.reports.MessageMetadataInput
import gov.cdc.ocio.processingstatusapi.models.reports.ReportInput
import gov.cdc.ocio.processingstatusapi.models.reports.StageInfoInput
import gov.cdc.ocio.processingstatusapi.models.submission.Issue
import gov.cdc.ocio.processingstatusapi.models.submission.Level
import gov.cdc.ocio.processingstatusapi.models.submission.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.submission.StageInfo

class ReportMutation() : CosmosLoader() {

    /**
     * Mutation for creating or replacing a report based on the provided action.
     *
     * @param input ReportInput
     * @param action Specifies whether to create or replace the report. Can be "create" or "replace".
     * @return Report
     * @throws BadRequestException
     * @throws ContentException
     * @throws BadStateException
     */
    fun upsertReport(input: ReportInput, action: String): Report? {
        logger.info("ReportId, id = ${input.id}, action = $action")

        return try {
            performUpsert(input, action)
        } catch (e: Exception) {
            logger.error("Error upserting report: ${e.message}", e)
            null
        }
    }

    private fun performUpsert(input: ReportInput, action: String): Report? {
        // Validate action parameter
        if (action !in listOf("create", "replace")) {
            throw BadRequestException("Invalid action specified: $action. Must be 'create' or 'replace'.")
        }

        val report = mapInputToReport(input)

        when (action) {
            "create" -> {
                if (!report.id.isNullOrBlank()) {
                    throw BadRequestException("ID should not be provided for create action.")
                }

                // Generate a new ID if not provided
                report.id = generateNewId()
                val options = CosmosItemRequestOptions()
                val createResponse: CosmosItemResponse<Report>? = reportsContainer?.createItem(report, PartitionKey(report.uploadId!!), options)
                return createResponse?.item
            }
            "replace" -> {
                if (report.id.isNullOrBlank()) {
                    throw BadRequestException("ID must be provided for replace action.")
                }

                // Attempt to read the existing item
                val readResponse: CosmosItemResponse<Report>? = reportsContainer?.readItem(report.id!!, PartitionKey(report.uploadId!!), Report::class.java)

                if (readResponse != null) {
                    // Replace the existing item
                    val options = CosmosItemRequestOptions()
                    val replaceResponse: CosmosItemResponse<Report>? = reportsContainer?.replaceItem(report, report.id!!, PartitionKey(report.uploadId!!), options)
                    return replaceResponse?.item
                } else {
                    throw BadRequestException("Report with ID ${report.id} not found for replacement.")
                }
            }
            else -> {
                throw BadRequestException("Unexpected action: $action")
            }
        }
    }

    private fun mapInputToReport(input: ReportInput): Report {
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
            content = input.content?.associate { it.key to it.value },
            timestamp = input.timestamp
        )
    }

    private fun mapInputToMessageMetadata(input: MessageMetadataInput): MessageMetadata {
        return MessageMetadata(
            messageUUID = input.messageUUID,
            messageHash = input.messageHash,
            aggregation = input.aggregation,
            messageIndex = input.messageIndex
        )
    }

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

    private fun mapInputToIssue(input: IssueInput): Issue {
        return Issue(
            level = input.code?.let { Level.valueOf(it) },
            message = input.description
        )
    }

    private fun generateNewId(): String {
        // Implement your logic to generate a new unique ID
        return java.util.UUID.randomUUID().toString()
    }
}
