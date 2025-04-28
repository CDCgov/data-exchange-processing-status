package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.toperrors.DataStreamTopErrorsNotificationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.toperrors.DataStreamTopErrorsNotificationWorkflow
import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import io.temporal.client.WorkflowClient
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * The main class which sets up and subscribes the workflow execution for digest counts and the frequency with which
 * each of the top 5 errors occur.
 *
 * @property logger KLogger
 * @property workflowEngine WorkflowEngine
 * @property notificationActivitiesImpl NotificationActivitiesImpl
 * @property description String
 */
class DataStreamTopErrorsNotificationSubscriptionService : KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val workflowEngine by inject<WorkflowEngine>()

    private val notificationActivitiesImpl = NotificationActivitiesImpl()

    private val description =
        """
        Determines the count of the top 5 errors that have occurred for this data stream in the time range provided.
        """.trimIndent()

    /**
     * The main method which gets called from the route which executes and kicks off the
     * workflow execution for digest counts and the frequency with which each of the top 5 errors occur
     *
     * @param subscription WorkflowSubscriptionForDataStreams
     */
    fun run(
        subscription: WorkflowSubscriptionForDataStreams
    ): WorkflowSubscriptionResult {

        val cronSchedule = subscription.cronSchedule
        val taskQueue = "dataStreamTopErrorsNotificationTaskQueue"

        val workflow = workflowEngine.setupWorkflow(
            description,
            taskQueue,
            cronSchedule,
            DataStreamTopErrorsNotificationWorkflowImpl::class.java,
            notificationActivitiesImpl,
            DataStreamTopErrorsNotificationWorkflow::class.java
        )

        val execution = WorkflowClient.start(
            workflow::checkDataStreamTopErrorsAndNotify,
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