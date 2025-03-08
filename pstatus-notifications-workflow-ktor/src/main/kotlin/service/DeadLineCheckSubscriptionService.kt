package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.model.DeadlineCheckSubscription
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
    fun run(subscription: DeadlineCheckSubscription):
            WorkflowSubscriptionResult {
        try {
            val dataStreamId = subscription.dataStreamId
            val jurisdiction = subscription.jurisdiction
            val dataStreamRoute = subscription.dataStreamRoute
            val cronSchedule = subscription.cronSchedule
            val deliveryReference = subscription.deliveryReference
            val taskQueue = "notificationTaskQueue"
            val workflow = workflowEngine.setupWorkflow(
                description,
                taskQueue,
                cronSchedule,
                NotificationWorkflowImpl::class.java,
                notificationActivitiesImpl,
                NotificationWorkflow::class.java
            )
            workflow?.let {
                val execution = WorkflowClient.start(
                    workflow::checkUploadAndNotify,
                    dataStreamId,
                    dataStreamRoute,
                    jurisdiction,
                    cronSchedule,
                    deliveryReference
                )
                return WorkflowSubscriptionResult(
                    subscriptionId = execution.workflowId,
                    message = "",
                    deliveryReference = ""
                )
            }
        }
        catch (e:Exception) {
            logger.error("Error occurred while subscribing workflow for upload deadline: ${e.message}")
        }
        throw Exception("Error occurred while executing workflow engine to subscribe for upload deadline")
    }
}