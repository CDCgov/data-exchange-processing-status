package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.dispatch.Dispatcher
import gov.cdc.ocio.processingnotifications.model.Subscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflowImpl
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
    private val taskQueue = "notificationTaskQueue"
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
        subscription: Subscription
    ): WorkflowSubscriptionResult {

//        val dataStreamId = subscription.dataStreamId
//        val jurisdiction = subscription.jurisdiction
//        val dataStreamRoute = subscription.dataStreamRoute
//        val cronSchedule = subscription.cronSchedule
//        val emailAddresses = subscription.emailAddresses

        val workflow = workflowEngine.setupWorkflow(
            description,
            taskQueue,
            subscription.cronSchedule,
            NotificationWorkflowImpl::class.java,
            notificationActivitiesImpl,
            NotificationWorkflow::class.java
        )

        val dispatcher = Dispatcher.fromSubscription(subscription)

        val execution = WorkflowClient.start(
            workflow::checkUploadAndNotify,
            subscription.dataStreamIds.first(),
            subscription.dataStreamRoutes.first(),
            subscription.jurisdictions.first(),
            subscription.cronSchedule,
            dispatcher
        )

        val workflowId = execution.workflowId
        return WorkflowSubscriptionResult(
            subscriptionId = workflowId,
            message = "Successfully subscribed for $workflowId",
            emailAddresses = subscription.emailAddresses,
            webhookUrl = subscription.webhookUrl
        )
    }
}