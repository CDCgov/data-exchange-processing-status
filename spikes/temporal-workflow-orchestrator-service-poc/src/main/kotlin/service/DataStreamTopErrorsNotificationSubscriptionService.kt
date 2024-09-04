package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingnotifications.model.DataStreamTopErrorsNotificationSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.DataStreamTopErrorsNotficationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.DataStreamTopErrorsNotificationWorkflow
import io.temporal.client.WorkflowClient

class DataStreamTopErrorsNotificationSubscriptionService {
    private val cacheService: InMemoryCacheService = InMemoryCacheService()
    private val workflowEngine:WorkflowEngine = WorkflowEngine()
    private val notificationActivitiesImpl:NotificationActivitiesImpl = NotificationActivitiesImpl()
    fun run(subscription: DataStreamTopErrorsNotificationSubscription):
            WorkflowSubscriptionResult {
        val dataStreamId = subscription.dataStreamId
        val dataStreamRoute = subscription.dataStreamRoute
        val jurisdiction = subscription.jurisdiction
        val daysToRun = subscription.daysToRun
        val timeToRun = subscription.timeToRun
        val deliveryReference= subscription.deliveryReference
        val taskQueue = "dataStreamTopErrorsNotificationTaskQueue"

        val workflow =  workflowEngine.setupWorkflow(taskQueue,daysToRun,timeToRun,
            DataStreamTopErrorsNotficationWorkflowImpl::class.java ,notificationActivitiesImpl, DataStreamTopErrorsNotificationWorkflow::class.java)

        val execution =  WorkflowClient.start(workflow::checkDataStreamTopErrorsAndNotify, dataStreamId, dataStreamRoute, jurisdiction,daysToRun, timeToRun, deliveryReference)
        return cacheService.updateSubscriptionPreferences(execution.workflowId,subscription)
    }
}