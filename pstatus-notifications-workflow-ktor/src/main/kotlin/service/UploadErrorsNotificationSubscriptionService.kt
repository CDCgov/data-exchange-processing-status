package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.model.UploadErrorsNotificationSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.UploadErrorsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.UploadErrorsNotificationWorkflowImpl

import io.temporal.client.WorkflowClient
import mu.KotlinLogging


/**
 * The main class which subscribes the workflow execution for upload errors.
 *
 * @property workflowEngine WorkflowEngine
 * @property notificationActivitiesImpl NotificationActivitiesImpl
 * @property logger KLogger
 */
class UploadErrorsNotificationSubscriptionService {
    private val logger = KotlinLogging.logger {}
    private val workflowEngine = WorkflowEngine()
    private val notificationActivitiesImpl = NotificationActivitiesImpl()

    /**
     * The main method which executes workflow engine to check for upload errors and notify.
     *
     * @param subscription UploadErrorsNotificationSubscription
     * @return WorkflowSubscriptionResult
     */
    fun run(subscription: UploadErrorsNotificationSubscription):
            WorkflowSubscriptionResult {
        try {
            val dataStreamId = subscription.dataStreamId
            val dataStreamRoute = subscription.dataStreamRoute
            val jurisdiction = subscription.jurisdiction
            val daysToRun = subscription.daysToRun
            val timeToRun = subscription.timeToRun
            val deliveryReference = subscription.deliveryReference
            val taskQueue = "uploadErrorsNotificationTaskQueue"

            val workflow = workflowEngine.setupWorkflow(
                taskQueue,
                daysToRun,
                timeToRun,
                UploadErrorsNotificationWorkflowImpl::class.java,
                notificationActivitiesImpl,
                UploadErrorsNotificationWorkflow::class.java
            )

            val execution = WorkflowClient.start(
                workflow::checkUploadErrorsAndNotify,
                dataStreamId,
                dataStreamRoute,
                jurisdiction,
                daysToRun,
                timeToRun,
                deliveryReference
            )
            logger.info("Started workflow with id: ${execution.workflowId}")
            return WorkflowSubscriptionResult(
                subscriptionId = execution.workflowId,
                message = "",
                deliveryReference = ""
            )
        }
        catch (e:Exception) {
            logger.error("Error occurred while checking for errors in upload: ${e.message}")
        }
        throw Exception("Error occurred while executing workflow engine to subscribe for errors in upload")
    }
}