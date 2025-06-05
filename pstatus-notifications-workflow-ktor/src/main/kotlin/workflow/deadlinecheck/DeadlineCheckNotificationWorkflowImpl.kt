package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import gov.cdc.ocio.processingnotifications.model.DeadlineCheck
import gov.cdc.ocio.processingnotifications.model.WebhookContent
import gov.cdc.ocio.processingnotifications.model.WorkflowType
import gov.cdc.ocio.processingnotifications.workflow.WorkflowActivity
import gov.cdc.ocio.types.model.NotificationType
import gov.cdc.ocio.types.model.WorkflowSubscriptionDeadlineCheck
import io.temporal.workflow.Workflow
import mu.KotlinLogging
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
class DeadlineCheckNotificationWorkflowImpl : DeadlineCheckNotificationWorkflow {

    private val logger = KotlinLogging.logger {}

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
        val triggeredAsString = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(triggered))

        try {
            val deadlineCheckRequest = DeadlineCheckRequest(
                dataStreamId,
                dataStreamRoute,
                expectedJurisdictions,
                workflowSubscription.deadlineTime
            )
            val response = activities.collectData(deadlineCheckRequest).getOrThrow() as DeadlineCheckResponse
            val deadlineCompliance = response.deadlineCompliance
            val jurisdictionCounts = response.jurisdictionCounts

            when (workflowSubscription.notificationType) {
                NotificationType.EMAIL -> emailAddresses?.let {
                    val body = DeadlineCheckEmailBuilder(
                        workflowId,
                        cronSchedule,
                        triggered,
                        dataStreamId,
                        dataStreamRoute,
                        expectedJurisdictions,
                        deadlineCompliance,
                        workflowSubscription.deadlineTime,
                        jurisdictionCounts
                    ).build()
                    activities.sendEmail(
                        emailAddresses,
                        "PHDO DEADLINE MISSED NOTIFICATION",
                        body
                    )
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
                            expectedJurisdictions,
                            deadlineCompliance,
                            DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(triggered)),
                            jurisdictionCounts
                        )
                    )
                    activities.sendWebhook(it, payload)
                }
            }
        } catch (e: Exception) {
            logger.error("Error occurred while checking for upload deadline: ${e.message}")
            throw Exception("Error occurred while checking for upload deadline")
        }
    }

}
