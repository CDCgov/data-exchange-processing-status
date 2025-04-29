package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.model.DeadlineCheck
import gov.cdc.ocio.processingnotifications.model.WebhookContent
import gov.cdc.ocio.processingnotifications.model.WorkflowType
import gov.cdc.ocio.processingnotifications.query.DeadlineCheckQuery
import gov.cdc.ocio.processingnotifications.workflow.WorkflowActivity
import gov.cdc.ocio.types.model.NotificationType
import gov.cdc.ocio.types.model.WorkflowSubscriptionDeadlineCheck
import io.temporal.failure.ActivityFailure
import io.temporal.workflow.Workflow
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.*
import java.time.format.DateTimeFormatter


/**
 * Implementation of the `DeadlineCheckNotificationWorkflow` interface, responsible for handling the process of
 * verifying upload deadlines and notifying subscribed entities if uploads are missing for specific jurisdictions.
 *
 * This workflow checks uploads associated with a given data stream and jurisdictions within a specified time frame.
 * Notifications are sent via email or webhook depending on the subscription's configuration.
 *
 * The class interacts with the Temporal Workflow engine, a `ProcessingStatusRepository` instance to handle data storage
 * and retrieval, and an activity stub for executing external tasks like sending notifications.
 *
 * Key Features:
 * - Verify if uploads occurred within a defined deadline.
 * - Identify jurisdictions with missing uploads.
 * - Send notifications using configured methods (email or webhook) when uploads are missing.
 */
class DeadlineCheckNotificationWorkflowImpl
    : DeadlineCheckNotificationWorkflow, KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val activities = WorkflowActivity.newDefaultActivityStub()

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
    override fun checkUploadDeadlinesAndNotify(
        workflowSubscription: WorkflowSubscriptionDeadlineCheck
    ) {
        val dataStreamId = workflowSubscription.dataStreamId
        val dataStreamRoute = workflowSubscription.dataStreamRoute
        val expectedJurisdictions = workflowSubscription.expectedJurisdictions
        val emailAddresses = workflowSubscription.emailAddresses
        val workflowId = Workflow.getInfo().workflowId
        val cronSchedule = Workflow.getInfo().cronSchedule
        val triggered = Workflow.getInfo().runStartedTimestampMillis
        val triggeredAsString = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(triggered));

        try {
            // Logic to check if the upload occurred
            val missingJurisdictions = performaDeadlineCheck(
                dataStreamId, dataStreamRoute, expectedJurisdictions, workflowSubscription.deadlineTime)
            if (missingJurisdictions.isNotEmpty()) {
                when (workflowSubscription.notificationType) {
                    NotificationType.EMAIL -> emailAddresses?.let {
                        val body = DeadlineCheckEmailBuilder(
                            workflowId,
                            cronSchedule,
                            triggered,
                            dataStreamId,
                            dataStreamRoute,
                            expectedJurisdictions
                        ).build()
                        workflowSubscription.emailAddresses?.let { activities.sendEmail(it, "PHDO DEADLINE MISSED NOTIFICATION", body) }
                    }
                    NotificationType.WEBHOOK -> workflowSubscription.webhookUrl?.let {
                        val payload = WebhookContent(
                            workflowId,
                            WorkflowType.UPLOAD_DEADLINE_CHECK,
                            workflowSubscription,
                            triggeredAsString,
                            DeadlineCheck(
                                dataStreamId,
                                dataStreamRoute,
                                missingJurisdictions,
                                DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(triggered))
                            )
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
     * @param jurisdictions The jurisdictions to check for.
     * @return True if an upload is found within the specified time range, otherwise false.
     */
    private fun performaDeadlineCheck(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdictions: List<String>,
        deadlineTime: LocalTime
    ): List<String> {
        val query = DeadlineCheckQuery.Builder(repository)
            .withDataStreamId(dataStreamId)
            .withDataStreamRoute(dataStreamRoute)
            .withExpectedJurisdictions(jurisdictions)
            .withDeadlineTime(deadlineTime)
            .build()

        try {
            val missingJurisdictions = query.run()
            logger.info("Missing jurisdictions: $missingJurisdictions")
            return missingJurisdictions
        } catch (ex: ActivityFailure) {
            logger.error("Error while processing daily upload digest. The workflow may have been canceled. Error: ${ex.localizedMessage}")
            throw ex
        } catch (ex: Exception) {
            logger.error("Error while processing daily upload digest: ${ex.localizedMessage}")
            throw ex
        }
    }

}
