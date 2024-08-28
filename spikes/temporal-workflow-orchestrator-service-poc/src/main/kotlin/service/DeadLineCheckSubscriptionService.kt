package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingnotifications.model.DeadlineCheckSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflowImpl
import io.temporal.client.WorkflowClient

class DeadLineCheckSubscriptionService {
    private val cacheService: InMemoryCacheService = InMemoryCacheService()
    private val workflowEngine: WorkflowEngine = WorkflowEngine()
    private val notificationActivitiesImpl:NotificationActivitiesImpl = NotificationActivitiesImpl()

    fun run(subscription: DeadlineCheckSubscription):
            WorkflowSubscriptionResult {
        val dataStreamId = subscription.dataStreamId
        val dataStreamRoute = subscription.dataStreamRoute
        val jurisdiction = subscription.jurisdiction
        val daysToRun = subscription.daysToRun
        val timeToRun = subscription.timeToRun
        val deliveryReference= subscription.deliveryReference
        val taskQueue = "notificationTaskQueue"


        val workflow =  workflowEngine.setupWorkflow(taskQueue,daysToRun,timeToRun,
            NotificationWorkflowImpl::class.java ,notificationActivitiesImpl, NotificationWorkflow::class.java)

        val execution =    WorkflowClient.start(workflow::checkUploadAndNotify, jurisdiction, dataStreamId, dataStreamRoute, daysToRun, timeToRun, deliveryReference)
       return cacheService.updateSubscriptionPreferences(execution.workflowId,subscription)
    }
}