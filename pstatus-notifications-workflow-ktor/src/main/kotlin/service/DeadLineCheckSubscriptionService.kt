package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckNotificationWorkflowImpl
import gov.cdc.ocio.types.model.WorkflowSubscription
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import io.temporal.client.WorkflowClient
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * The main class which subscribes the workflow execution for upload deadline check.
 *
 * @property logger KLogger
 * @property workflowEngine WorkflowEngine
 * @property notificationActivitiesImpl NotificationActivitiesImpl
 */
class DeadLineCheckSubscriptionService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val workflowEngine by inject<WorkflowEngine>()

    private val notificationActivitiesImpl = NotificationActivitiesImpl()

    private val description =
        """
        Checks to see if all the expected uploads have occurred by the deadline provided.
        """.trimIndent()

    /**
     *  The main method which executes workflow for uploadDeadline check.
     *
     *  @param subscription DeadlineCheckSubscription
     *  @return WorkflowSubscriptionResult
     */
    fun run(
        subscription: WorkflowSubscription
    ): WorkflowSubscriptionResult {

        val cronSchedule = subscription.cronSchedule
        val taskQueue = "deadlineCheckNotificationTaskQueue"

        val workflow = workflowEngine.setupWorkflow(
            description,
            taskQueue,
            cronSchedule,
            DeadlineCheckNotificationWorkflowImpl::class.java,
            notificationActivitiesImpl,
            DeadlineCheckNotificationWorkflow::class.java
        )

        val execution = WorkflowClient.start(
            workflow::checkUploadDeadlinesAndNotify,
            subscription
        )

        return WorkflowSubscriptionResult(
            subscriptionId = execution.workflowId,
            message = "Successfully subscribed",
            emailAddresses = subscription.emailAddresses,
            webhookUrl = subscription.webhookUrl
        )
    }
}