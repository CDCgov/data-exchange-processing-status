package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.model.CheckUploadResponse
import gov.cdc.ocio.processingnotifications.model.DeadlineCheck
import gov.cdc.ocio.processingnotifications.model.WebhookContent
import gov.cdc.ocio.processingnotifications.model.WorkflowType
import gov.cdc.ocio.processingnotifications.utils.SqlClauseBuilder
import gov.cdc.ocio.types.model.NotificationType
import gov.cdc.ocio.types.model.WorkflowSubscription
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.*
import java.time.format.DateTimeFormatter


/**
 * The implementation class for notifying if an upload has not occurred by a specified time.
 *
 * @property repository ProcessingStatusRepository
 * @property logger KLogger
 * @property activities (NotificationActivities..NotificationActivities?)
 */
class DeadlineCheckNotificationWorkflowImpl : DeadlineCheckNotificationWorkflow, KoinComponent {

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
     * The function invoked by the Temporal Workflow engine to check if an upload occurred within a specified time.
     *
     * If the upload has not occurred, it triggers a notification based on the subscription's configured notification
     * type (email or webhook). For email notifications, the specified email addresses are notified. For webhook
     * notifications, a webhook payload is sent to the configured webhook URL.
     *
     * @param workflowSubscription The subscription containing workflow-related configuration such as data stream ID,
     * jurisdiction, email addresses, webhook URL, and notification type.
     */
    override fun checkUploadAndNotify(
        workflowSubscription: WorkflowSubscription
    ) {
        val dataStreamId = workflowSubscription.dataStreamIds.first()
        val jurisdiction = workflowSubscription.jurisdictions.first()
        val emailAddresses = workflowSubscription.emailAddresses

        try {
            // Logic to check if the upload occurred*/
            val uploadOccurred = checkUpload(dataStreamId, jurisdiction)
            if (!uploadOccurred) {
                when (workflowSubscription.notificationType) {
                    NotificationType.EMAIL -> emailAddresses?.let { activities.sendNotification(dataStreamId, jurisdiction, emailAddresses) }
                    NotificationType.WEBHOOK -> workflowSubscription.webhookUrl?.let {
                        val subId = Workflow.getInfo().workflowId
                        val triggered = Workflow.getInfo().runStartedTimestampMillis
                        val payload = WebhookContent(
                            subId,
                            WorkflowType.UPLOAD_DEADLINE_CHECK,
                            workflowSubscription,
                            DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(triggered)),
                            DeadlineCheck(dataStreamId, jurisdiction, LocalDate.now().toString())
                        )
                        activities.sendWebhook(it, payload)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error occurred while checking for upload deadline: ${e.message}")
            throw Exception("Error occurred while checking for upload deadline")
        }
    }

    /**
     * Checks whether an upload has occurred within a specified time range for the given data stream and jurisdiction.
     *
     * The method generates a query to search for uploads in the database, using today's date and a specific
     * start and end time range in UTC. If any matching results are found, the method returns true; otherwise, false.
     * In case of any errors during execution, an exception is thrown.
     *
     * @param dataStreamId The identifier of the data stream to check for uploads.
     * @param jurisdiction The jurisdiction associated with the data stream.
     * @return True if an upload is found within the specified time range, otherwise false.
     */
    private fun checkUpload(dataStreamId: String, jurisdiction: String): Boolean {
        /** Get today's date in UTC **/
        val today = LocalDate.now(ZoneId.of("UTC"))
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        val reportsCollection = repository.reportsCollection
        val collectionName = reportsCollection.collectionNameForQuery
        val cVar = reportsCollection.collectionVariable
        val cPrefix = reportsCollection.collectionVariablePrefix

        val dateStart = today.atStartOfDay(ZoneOffset.UTC).format(formatter)
        val dateEnd = today.atTime(12, 0, 0).atZone(ZoneOffset.UTC).format(formatter)
        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(null, dateStart, dateEnd, cPrefix)

        val notificationQuery = """
           SELECT ${cPrefix}id 
           FROM $collectionName $cVar 
           WHERE ${cPrefix}dataStreamId = '$dataStreamId' 
               AND ${cPrefix}jurisdiction = '$jurisdiction' 
               AND $timeRangeWhereClause 
           """.trimIndent()

        logger.info("notification Query: $notificationQuery")

        return try {
            val results = repository.reportsCollection.queryItems(
                notificationQuery,
                CheckUploadResponse::class.java
            )
            logger.info("notification Query results: ${results.size}")
            results.isNotEmpty() // Returns true if there are results, false if none
        } catch (ex: Exception) {
            logger.error("Error occurred while checking upload for $dataStreamId and $jurisdiction: ${ex.message}")
            throw Exception("Error occurred in checking upload")
        }
    }

}
