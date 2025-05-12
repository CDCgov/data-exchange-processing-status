package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.processingnotifications.model.UploadErrorSummary
import gov.cdc.ocio.processingnotifications.model.WebhookContent
import gov.cdc.ocio.processingnotifications.model.WorkflowType
import gov.cdc.ocio.processingnotifications.workflow.WorkflowActivity
import gov.cdc.ocio.types.model.NotificationType
import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
import io.temporal.workflow.Workflow
import mu.KotlinLogging
import java.time.Instant
import java.time.format.DateTimeFormatter


/**
 * Implementation of the `DataStreamTopErrorsNotificationWorkflow` interface.
 * This class manages the workflow for checking data stream errors and notifying the relevant parties
 * through various notification mechanisms (e.g., email, webhook). It processes error types, tallies counts,
 * and formats output for notification.
 */
class DataStreamTopErrorsNotificationWorkflowImpl : DataStreamTopErrorsNotificationWorkflow {

    private val logger = KotlinLogging.logger {}

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
            val topErrorsRequest = TopErrorsRequest(
                dataStreamId,
                dataStreamRoute,
                dayInterval
            )
            val response = activities.collectData(topErrorsRequest).getOrThrow() as TopErrorsResponse
            val workflowId = Workflow.getInfo().workflowId
            val cronSchedule = Workflow.getInfo().cronSchedule

            when (workflowSubscription.notificationType) {
                NotificationType.EMAIL -> workflowSubscription.emailAddresses?.let {
                    val body = TopErrorsEmailBuilder(
                        workflowId,
                        cronSchedule,
                        dataStreamId,
                        dataStreamRoute,
                        response.failedMetadataValidationCount,
                        response.failedDeliveryCount,
                        response.delayedUploads,
                        response.delayedDeliveries,
                        response.abandonedUploads,
                        dayInterval
                    ).build()
                    activities.sendEmail(it, "PHDO TOP ERRORS NOTIFICATION", body)
                }
                NotificationType.WEBHOOK -> workflowSubscription.webhookUrl?.let {
                    val subId = Workflow.getInfo().workflowId
                    val triggered = Workflow.getInfo().runStartedTimestampMillis
                    val payload = WebhookContent(
                        subId,
                        WorkflowType.UPLOAD_ERROR_SUMMARY,
                        workflowSubscription,
                        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(triggered)),
                        UploadErrorSummary(
                            response.failedMetadataValidationCount,
                            response.failedDeliveryCount,
                            response.delayedUploads,
                            response.delayedDeliveries,
                            response.abandonedUploads
                        )
                    )
                    activities.sendWebhook(it, payload)
                }
            }
        } catch (e: Exception) {
            logger.error("Error occurred while checking for counts and top errors and frequency in an upload: ${e.message}")
        }
    }

}
