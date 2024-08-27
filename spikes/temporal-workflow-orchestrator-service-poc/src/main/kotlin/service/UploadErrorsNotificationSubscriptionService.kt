package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingnotifications.model.UploadErrorsNotificationSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.model.getCronExpression
import gov.cdc.ocio.processingnotifications.workflow.UploadErrorsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.UploadErrorsNotificationWorkflowImpl

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory


class UploadErrorsNotificationSubscriptionService {
    private val cacheService: InMemoryCacheService = InMemoryCacheService()
    fun run(subscription: UploadErrorsNotificationSubscription):
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
        val taskQueue = "uploadErrorsNotificationTaskQueue"

        val worker = factory.newWorker(taskQueue)
        worker.registerWorkflowImplementationTypes(UploadErrorsNotificationWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(NotificationActivitiesImpl())

        factory.start()

        val workflowOptions = WorkflowOptions.newBuilder()
            .setTaskQueue(taskQueue)
            .setCronSchedule(getCronExpression(daysToRun,timeToRun)) // Cron schedule: 15 5 * * 1-5 - Every week day at  5:15a
            .build()

        val workflow = client.newWorkflowStub(
            UploadErrorsNotificationWorkflow::class.java,
            workflowOptions
        )
        val execution =  WorkflowClient.start(workflow::checkUploadErrorsAndNotify, dataStreamId, dataStreamRoute, jurisdiction,daysToRun, timeToRun, deliveryReference)
        return cacheService.updateSubscriptionPreferences(execution.workflowId,subscription)
    }
}