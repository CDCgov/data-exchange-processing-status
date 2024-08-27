package gov.cdc.ocio.processingnotifications.service


import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingnotifications.model.DeadlineCheckSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.model.getCronExpression
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflowImpl

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory


class DeadLineCheckSubscriptionService {
    private val cacheService: InMemoryCacheService = InMemoryCacheService()
    fun run(subscription: DeadlineCheckSubscription):
            WorkflowSubscriptionResult {
        val dataStreamId = subscription.dataStreamId
        val dataStreamRoute = subscription.dataStreamRoute
        val jurisdiction = subscription.jurisdiction
        val daysToRun = subscription.daysToRun
        val timeToRun = subscription.timeToRun
        val deliveryReference= subscription.deliveryReference

        val service = WorkflowServiceStubs.newLocalServiceStubs()
        val client = WorkflowClient.newInstance(service)
        val factory = WorkerFactory.newInstance(client)
        val taskQueue = "notificationTaskQueue"

        val worker = factory.newWorker(taskQueue)
        worker.registerWorkflowImplementationTypes(NotificationWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(NotificationActivitiesImpl())

        factory.start()

        val workflowOptions = WorkflowOptions.newBuilder()
            .setTaskQueue(taskQueue)
            .setCronSchedule(getCronExpression(daysToRun,timeToRun)) // Cron schedule: 15 5 * * 1-5 - Every week day at  5:15a
            .build()

        val workflow = client.newWorkflowStub(
            NotificationWorkflow::class.java,
            workflowOptions
        )
       val execution =    WorkflowClient.start(workflow::checkUploadAndNotify, jurisdiction, dataStreamId, dataStreamRoute, daysToRun, timeToRun, deliveryReference)
       return cacheService.updateSubscriptionPreferences(execution.workflowId,subscription)
    }
}