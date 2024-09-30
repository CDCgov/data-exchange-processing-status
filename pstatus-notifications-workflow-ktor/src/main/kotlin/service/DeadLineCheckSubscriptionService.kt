package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingnotifications.model.DeadlineCheckSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflowImpl
import io.temporal.client.WorkflowClient
import mu.KotlinLogging

/**
 * The main class which subscribes the workflow execution
 * for upload deadline check
 * @property cacheService IMemoryCacheService
 * @property workflowEngine WorkflowEngine
 * @property notificationActivitiesImpl  NotificationActivitiesImpl
 */
class DeadLineCheckSubscriptionService {
    private val logger = KotlinLogging.logger {}
    private val cacheService: InMemoryCacheService = InMemoryCacheService()
    private val workflowEngine: WorkflowEngine = WorkflowEngine()
    private val notificationActivitiesImpl:NotificationActivitiesImpl = NotificationActivitiesImpl()

    /**
     *  The main method which executes workflow for uploadDeadline check
     *  @param subscription DeadlineCheckSubscription
     *  @return WorkflowSubscriptionResult
     */
    fun run(subscription: DeadlineCheckSubscription):
            WorkflowSubscriptionResult {
        try {
            val dataStreamId = subscription.dataStreamId
            val jurisdiction = subscription.jurisdiction
            val daysToRun = subscription.daysToRun
            val timeToRun = subscription.timeToRun
            val deliveryReference= subscription.deliveryReference
            val taskQueue = "notificationTaskQueue"
            val workflow :NotificationWorkflow =  workflowEngine.setupWorkflow(taskQueue,daysToRun,timeToRun,
                NotificationWorkflowImpl::class.java ,notificationActivitiesImpl, NotificationWorkflow::class.java)
            val execution =    WorkflowClient.start(workflow::checkUploadAndNotify, dataStreamId, jurisdiction, daysToRun, timeToRun, deliveryReference)
            return cacheService.updateSubscriptionPreferences(execution.workflowId,subscription)
        }
        catch (e:Exception){
            logger.error("Error occurred while subscribing workflow for upload deadline: ${e.message}")
        }
        throw Exception("Error occurred while executing workflow engine to subscribe for upload deadline")
    }
}