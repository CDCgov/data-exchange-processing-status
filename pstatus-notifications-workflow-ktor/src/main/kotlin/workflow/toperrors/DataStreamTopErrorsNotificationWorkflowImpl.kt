package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.processingnotifications.model.ErrorDetail
import gov.cdc.ocio.processingnotifications.model.UploadErrorSummary
import gov.cdc.ocio.processingnotifications.model.WebhookContent
import gov.cdc.ocio.processingnotifications.model.WorkflowType
import gov.cdc.ocio.processingnotifications.service.ReportService
import gov.cdc.ocio.processingnotifications.workflow.WorkflowActivity
import gov.cdc.ocio.types.model.NotificationType
import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
import io.temporal.workflow.Workflow
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import mu.KotlinLogging
import java.time.Instant
import java.time.format.DateTimeFormatter


/**
 * Implementation of the `DataStreamTopErrorsNotificationWorkflow` interface.
 * This class manages the workflow for checking data stream errors and notifying the relevant parties
 * through various notification mechanisms (e.g., email, webhook). It processes error types, tallies counts,
 * and formats output for notification.
 */
class DataStreamTopErrorsNotificationWorkflowImpl
    : DataStreamTopErrorsNotificationWorkflow {

    private val logger = KotlinLogging.logger {}
    private val reportService = ReportService()

    private val activities = WorkflowActivity.newDefaultActivityStub()

    /**
     * The function which determines the digest counts and top errors during an upload and its frequency.
     *
     * @param workflowSubscription WorkflowSubscription
     */
    override fun checkDataStreamTopErrorsAndNotify(
        workflowSubscription: WorkflowSubscriptionForDataStreams
    ) {
        val dayInterval = workflowSubscription.sinceDays
        val dataStreamId = workflowSubscription.dataStreamIds.first()
        val dataStreamRoute = workflowSubscription.dataStreamRoutes.first()
        try {
            // Logic to check if the upload occurred*/
            val failedMetadataVerifyCount = reportService.countFailedReports(dataStreamId, dataStreamRoute, StageAction.METADATA_VERIFY, dayInterval)
            val failedDeliveryCount = reportService.countFailedReports(dataStreamId, dataStreamRoute, StageAction.FILE_DELIVERY, dayInterval)
            val delayedUploads = reportService.getDelayedUploads(dataStreamId, dataStreamRoute, dayInterval)
            val delayedDeliveries = reportService.getDelayedDeliveries(dataStreamId, dataStreamRoute, dayInterval)

            when (workflowSubscription.notificationType) {
                NotificationType.EMAIL -> {
                    val body = formatEmailBody(
                        dataStreamId,
                        dataStreamRoute,
                        failedMetadataVerifyCount,
                        failedDeliveryCount,
                        delayedUploads,
                        delayedDeliveries,
                        dayInterval
                    )
                    workflowSubscription.emailAddresses?.let { activities.sendEmail(it, "PHDO TOP ERRORS NOTIFICATION", body) }
                }
                NotificationType.WEBHOOK -> workflowSubscription.webhookUrl?.let {
                    val subId = Workflow.getInfo().workflowId
                    val triggered = Workflow.getInfo().runStartedTimestampMillis
                    val payload = WebhookContent(
                        subId,
                        WorkflowType.UPLOAD_ERROR_SUMMARY,
                        workflowSubscription,
                        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(triggered)),
                        UploadErrorSummary(failedMetadataVerifyCount, failedDeliveryCount, delayedUploads, delayedDeliveries)
                    )
                    activities.sendWebhook(it, payload)
                }
            }
        } catch (e: Exception) {
            logger.error("Error occurred while checking for counts and top errors and frequency in an upload: ${e.message}")
        }
    }

    /**
     * Builds the HTML string of the email body
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param failedMetadataValidationCount Int
     * @param failedDeliveryCount Int
     * @param delayedUploads List<String>
     * @param delayedDeliveries List<String>
     * @param daysInterval Int
     */
    private fun formatEmailBody(
        dataStreamId: String,
        dataStreamRoute: String,
        failedMetadataValidationCount: Int,
        failedDeliveryCount: Int,
        delayedUploads: List<String>,
        delayedDeliveries: List<String>,
        daysInterval: Int
    ): String {
        return buildString {
            appendHTML().html {
                body {
                    h2 { +"$dataStreamId $dataStreamRoute Upload Issues in the last $daysInterval days" }
                    br {  }
                    h3 { +"Total: ${failedMetadataValidationCount + failedDeliveryCount + delayedUploads.size + delayedDeliveries.size }" }
                    ul {
                        li { +"Failed Metadata Validation: $failedMetadataValidationCount" }
                        li { +"Failed Deliveries: $failedDeliveryCount" }
                        li { +"Delayed Uploads: ${delayedUploads.size}" }
                        li { +"Delayed Deliveries: ${delayedDeliveries.size}" }
                    }
                    br {  }
                    h3 { +"Delayed Uploads" }
                    ul {
                        delayedUploads.map { li { +it } }
                    }
                    br {  }
                    h3 { +"Delayed Deliveries" }
                    ul {
                        delayedDeliveries.map{ li { +it }}
                    }
                }
            }
        }
    }

    /**
     * Function which actually does find the counts and the erroneous fields and its frequency.
     *
     * @param errors List<String>
     * @return Pair<int, List<ErrorDetail>
     */
    private fun getTopErrors(errors: List<String>): Pair<Int, List<ErrorDetail>> {
        // Group the errors by description and count their occurrences
        val errorCounts = errors.groupingBy { it }.eachCount()

        // Convert the grouped data into a list of ErrorDetail objects
        val errorDetails = errorCounts.map { (description, count) ->
            ErrorDetail(description, count)
        }
        // Sort the errors by their count in descending order and take the top 5
        val topErrors = errorDetails.sortedByDescending { it.count }.take(5)

        // Return the total count of errors and the top 5 errors
        return Pair(errors.size, topErrors)
    }

}
