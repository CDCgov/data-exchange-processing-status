package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.model.UploadDigest
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscription
import gov.cdc.ocio.processingnotifications.query.*
import gov.cdc.ocio.types.model.NotificationType
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.failure.ActivityFailure
import io.temporal.workflow.Workflow
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * The implementation class which determines the daily digest counts of the list of jurisdictions for the set data
 * stream id list.
 *
 * @property repository ProcessingStatusRepository
 * @property logger logger
 * @property activities T
 */
class UploadDigestCountsNotificationWorkflowImpl :
    UploadDigestCountsNotificationWorkflow, KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val activities = Workflow.newActivityStub(
        NotificationActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10)) // Set the start-to-close timeout
            .setScheduleToCloseTimeout(Duration.ofMinutes(1)) // Set the schedule-to-close timeout
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setMaximumAttempts(3) // Set retry options if needed
                    .build()
            )
            .build()
    )

    /**
     * The main function which is used by temporal workflow engine for orchestrating the daily upload digest counts.
     *
     * @param numDaysAgoToRun Long
     * @param dataStreamIds List<String>
     * @param dataStreamRoutes List<String>
     * @param jurisdictions List<String>
     * @param emailAddresses List<String>
     */
    override fun processDailyUploadDigest(
        subscription: WorkflowSubscription
    ) {
        try {
            val utcDateToRun = LocalDate.now().minusDays(subscription.sinceDays.toLong())
            val formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy")

            // Upload digest query to get all the counts by data stream id, data stream route, and jurisdiction
            val uploadDigestQuery = UploadDigestCountsQuery.Builder(repository)
                .withDataStreamIds(subscription.dataStreamIds)
                .withDataStreamRoutes(subscription.dataStreamRoutes)
                .withJurisdictions(subscription.jurisdictions)
                .withUtcToRun(utcDateToRun)
                .build()
            val uploadDigestResults = uploadDigestQuery.run()

            // Aggregate the upload counts
            val aggregatedCounts = aggregateUploadCounts(uploadDigestResults)

            // Get the upload metrics
            val uploadMetricsQuery = UploadMetricsQuery.Builder(repository)
                .withDataStreamIds(subscription.dataStreamIds)
                .withDataStreamRoutes(subscription.dataStreamRoutes)
                .withJurisdictions(subscription.jurisdictions)
                .withUtcToRun(utcDateToRun)
                .build()
            val uploadMetrics = uploadMetricsQuery.run()

            // Get the upload and delivery durations
            val uploadDurationsQuery = UploadDurationQuery.Builder(repository)
                .withDataStreamIds(subscription.dataStreamIds)
                .withDataStreamRoutes(subscription.dataStreamRoutes)
                .withJurisdictions(subscription.jurisdictions)
                .withUtcToRun(utcDateToRun)
                .build()
            val uploadDurations = uploadDurationsQuery.run()

            // Format the email body
            val workflowId = Workflow.getInfo().workflowId
            val cronSchedule = Workflow.getInfo().cronSchedule
            val dateRun = utcDateToRun.format(formatter)
            val emailBody = UploadDigestCountsEmailBuilder(
                workflowId, cronSchedule, subscription.dataStreamIds, subscription.dataStreamRoutes, subscription.jurisdictions,
                dateRun, aggregatedCounts, uploadMetrics, uploadDurations
            ).build()
            logger.info("Sending upload digest counts email")

            when (subscription.notificationType) {
                NotificationType.EMAIL -> subscription.emailAddresses?.let { activities.sendDigestEmail(emailBody, subscription.emailAddresses) }
                NotificationType.WEBHOOK -> subscription.webhookUrl?.let { activities.sendWebhook(it, UploadDigest(aggregatedCounts, uploadMetrics, uploadDurations)) }
            }
        } catch (ex: ActivityFailure) {
            logger.error("Error while processing daily upload digest. The workflow may have been canceled. Error: ${ex.localizedMessage}")
        } catch (ex: Exception) {
            logger.error("Error while processing daily upload digest: ${ex.localizedMessage}")
            throw ex
        }
    }

    /**
     *  DynamoDB does not support GROUP BY, so this function is used to aggregate the counts
     *  Function that groups by jurisdictionId and dataStreamId to aggregate counts.
     *
     *  @param uploadCounts List<UploadDigestResponse>
     *  @return UploadDigestCounts
     */
    private fun aggregateUploadCounts(
        uploadCounts: List<UploadDigestResponse>
    ): UploadDigestCounts {

        val digest = uploadCounts.groupBy { it.dataStreamId }
            .mapValues { (_, dataStreamCounts) ->
                dataStreamCounts.groupBy { it.dataStreamRoute }
                    .mapValues { (_, routeCounts) ->
                        routeCounts.groupBy { it.jurisdiction }
                            .mapValues { (_, jurisdictionCounts) ->
                                Counts(
                                    jurisdictionCounts.sumOf { it.started },
                                    jurisdictionCounts.sumOf { it.completed },
                                    jurisdictionCounts.sumOf { it.failedDelivery },
                                    jurisdictionCounts.sumOf { it.delivered }
                                )
                            }
                    }
            }
        return UploadDigestCounts(digest)
    }

}
