package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.model.DataStreamTopErrorsNotificationSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.DataStreamTopErrorsNotificationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.DataStreamTopErrorsNotificationWorkflow
import io.temporal.client.WorkflowClient
import mu.KotlinLogging


/**
 * The main class which sets up and subscribes the workflow execution
 * for digest counts and the frequency with which each of the top 5 errors occur
 */
class DataStreamTopErrorsNotificationSubscriptionService {
    private val logger = KotlinLogging.logger {}
    private val workflowEngine = WorkflowEngine()
    private val notificationActivitiesImpl = NotificationActivitiesImpl()

    /**
     * The main method which gets called from the route which executes and kicks off the
     * workflow execution for digest counts and the frequency with which each of the top 5 errors occur
     *
     * @param subscription DataStreamTopErrorsNotificationSubscription
     */
    fun run(subscription: DataStreamTopErrorsNotificationSubscription): WorkflowSubscriptionResult {
        try {
            val dataStreamId = subscription.dataStreamId
            val dataStreamRoute = subscription.dataStreamRoute
            val jurisdiction = subscription.jurisdiction
            val daysToRun = subscription.daysToRun
            val timeToRun = subscription.timeToRun
            val deliveryReference= subscription.deliveryReference
            val taskQueue = "dataStreamTopErrorsNotificationTaskQueue"

            val workflow = workflowEngine.setupWorkflow(
                taskQueue,
                daysToRun,
                timeToRun,
                DataStreamTopErrorsNotificationWorkflowImpl::class.java,
                notificationActivitiesImpl,
                DataStreamTopErrorsNotificationWorkflow::class.java
            )

            val execution = WorkflowClient.start(
                workflow::checkDataStreamTopErrorsAndNotify,
                dataStreamId,
                dataStreamRoute,
                jurisdiction,
                daysToRun,
                timeToRun,
                deliveryReference
            )
            return WorkflowSubscriptionResult(
                subscriptionId = execution.workflowId,
                message = "",
                deliveryReference = ""
            )
        }
        catch (e:Exception) {
            logger.error("Error occurred while subscribing for digest counts and top errors: ${e.message}")
        }
        throw Exception("Error occurred while subscribing for the workflow engine for digest counts and top errors")
    }
}