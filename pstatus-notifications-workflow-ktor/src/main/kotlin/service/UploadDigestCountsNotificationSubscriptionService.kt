package gov.cdc.ocio.processingnotifications.service



import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.model.UploadDigestSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.UploadDigestCountsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.UploadDigestCountsNotificationWorkflowImpl
import io.temporal.client.WorkflowClient
import mu.KotlinLogging

/**
 * The main class which subscribes the workflow execution
 * for upload digest counts
 * @property logger KotlinLogging.Logger
 * @property workflowEngine WorkflowEngine
 * @property notificationActivitiesImpl  NotificationActivitiesImpl
 */
class UploadDigestCountsNotificationSubscriptionService {
    private val logger = KotlinLogging.logger {}
    private val workflowEngine: WorkflowEngine = WorkflowEngine()
    private val notificationActivitiesImpl:NotificationActivitiesImpl = NotificationActivitiesImpl()

    /**
     *  The main method which executes workflow for uploadDeadline check
     *  @param subscription DeadlineCheckSubscription
     *  @return WorkflowSubscriptionResult
     */
    fun run(subscription: UploadDigestSubscription):
            WorkflowSubscriptionResult {
        try {
            val dataStreams= subscription.dataStreamIds
            val jurisdictionIds = subscription.jurisdictionIds
            val daysToRun = subscription.daysToRun
            val timeToRun = subscription.timeToRun
            val deliveryReference= subscription.deliveryReference
            val taskQueue = "uploadDigestCountsTaskQueue"
            val workflow :UploadDigestCountsNotificationWorkflow=  workflowEngine.setupWorkflow(taskQueue,daysToRun,timeToRun,
                UploadDigestCountsNotificationWorkflowImpl::class.java ,notificationActivitiesImpl, UploadDigestCountsNotificationWorkflow::class.java)
            val execution =    WorkflowClient.start(workflow::processDailyUploadDigest, jurisdictionIds, dataStreams, deliveryReference)
          //  return cacheService.updateSubscriptionPreferences(execution.workflowId,subscription)
            val workflowId = execution.workflowId
            return WorkflowSubscriptionResult(subscriptionId = workflowId, message = "Successfully subscribed for $workflowId", deliveryReference = deliveryReference)
        }
        catch (e:Exception){
            logger.error("Error occurred while subscribing workflow for daily upload digest counts: ${e.message}")
        }
        throw Exception("Error occurred while executing workflow engine to subscribe for daily upload digest counts")
    }
}