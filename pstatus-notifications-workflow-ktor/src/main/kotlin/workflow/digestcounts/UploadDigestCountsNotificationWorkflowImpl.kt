package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import gov.cdc.ocio.processingnotifications.model.UploadDigest
import gov.cdc.ocio.processingnotifications.model.WebhookContent
import gov.cdc.ocio.processingnotifications.model.WorkflowType
import gov.cdc.ocio.processingnotifications.workflow.WorkflowActivity
import gov.cdc.ocio.types.model.NotificationType
import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
import io.temporal.failure.ActivityFailure
import io.temporal.workflow.Workflow
import mu.KotlinLogging
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * Implementation of the `UploadDigestCountsNotificationWorkflow` interface designed to process
 * and dispatch daily upload digest notifications. The workflow aggregates upload counts,
 * metrics, and durations, formatting the data for either email or webhook delivery based on the
 * subscriber's configuration.
 *
 * This class leverages Temporal Workflow functionality to process complex asynchronous workflows
 * and includes error handling to manage scenarios such as activity failures or data inconsistencies.
 */
class UploadDigestCountsNotificationWorkflowImpl : UploadDigestCountsNotificationWorkflow {

    private val logger = KotlinLogging.logger {}

    private val activities = WorkflowActivity.newDefaultActivityStub()

    private val formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy")

    /**
     * Processes the daily upload digest based on the provided subscription details.
     * This method retrieves and aggregates upload counts, metrics, and durations, formats the information,
     * and sends notifications via email or webhook depending on the subscription's configuration.
     *
     * @param subscription The subscription details, including data stream IDs, routes, jurisdictions,
     * notification type, and other relevant configurations for processing the daily upload digest.
     */
    override fun processDailyUploadDigest(
        subscription: WorkflowSubscriptionForDataStreams
    ) {
        try {
            val utcDateToRun = LocalDate.now().minusDays(subscription.sinceDays.toLong())

            val uploadDigestCountsRequest = UploadDigestCountsRequest(
                subscription.dataStreamIds,
                subscription.dataStreamRoutes,
                subscription.jurisdictions,
                subscription.sinceDays,
                utcDateToRun
            )
            val response = activities.collectData(uploadDigestCountsRequest).getOrThrow() as UploadDigestCountsResponse

            // Finally, dispatch the notification
            dispatchNotification(
                subscription,
                utcDateToRun,
                response.aggregatedCounts,
                response.uploadMetrics,
                response.uploadDurations
            )
        } catch (ex: ActivityFailure) {
            logger.error("Error while processing daily upload digest. The workflow may have been canceled. Error: ${ex.localizedMessage}")
        } catch (ex: Exception) {
            logger.error("Error while processing daily upload digest: ${ex.localizedMessage}")
            throw ex
        }
    }

    /**
     * Dispatches a notification for the given workflow subscription, based on its configured notification type.
     * The method formats and sends either an email notification or a webhook payload containing aggregated upload
     * digest information and metrics.
     *
     * @param subscription The workflow subscription configuration, including metadata for data streams, jurisdictions,
     *                     notification type, email addresses, and webhook URL.
     * @param utcDateToRun The date for which the upload digest is being generated and sent.
     * @param aggregatedCounts The aggregated counts from the upload digest, grouped by data stream ID, route, and jurisdiction.
     * @param uploadMetrics Metrics related to upload and delivery, such as min, max, mean, and median values for durations and file sizes.
     * @param uploadDurations List of upload durations, used for additional notification or payload formatting.
     */
    private fun dispatchNotification(
        subscription: WorkflowSubscriptionForDataStreams,
        utcDateToRun: LocalDate,
        aggregatedCounts: UploadDigestCounts,
        uploadMetrics: UploadMetrics,
        uploadDurations: List<Long>
    ) {
        // Format the email body
        val workflowId = Workflow.getInfo().workflowId
        val cronSchedule = Workflow.getInfo().cronSchedule
        val dateRun = utcDateToRun.format(formatter)

        when (subscription.notificationType) {
            NotificationType.EMAIL -> {
                val emailBody = UploadDigestCountsEmailBuilder(
                    workflowId,
                    cronSchedule,
                    subscription.dataStreamIds,
                    subscription.dataStreamRoutes,
                    subscription.jurisdictions,
                    dateRun,
                    aggregatedCounts,
                    uploadMetrics,
                    uploadDurations
                ).build()
                logger.info("Sending upload digest counts email")
                subscription.emailAddresses?.let { activities.sendEmail(it, "PHDO UPLOAD DIGEST NOTIFICATION", emailBody) }
            }

            NotificationType.WEBHOOK -> subscription.webhookUrl?.let {
                val subId = Workflow.getInfo().workflowId
                val triggered = Workflow.getInfo().runStartedTimestampMillis
                val payload = WebhookContent(
                    subId,
                    WorkflowType.UPLOAD_DIGEST,
                    subscription,
                    DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(triggered)),
                    UploadDigest(aggregatedCounts, uploadMetrics, uploadDurations)
                )
                activities.sendWebhook(it, payload)
            }
        }
    }

}
