package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.DeadlineCheckSubscription
import gov.cdc.ocio.processingnotifications.DeadlineCheckSubscriptionResult
import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflowImpl

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory


class DeadLineCheckNotificationService {
    private val cacheService: InMemoryCacheService = InMemoryCacheService()
    fun run(subscription: DeadlineCheckSubscription):
            DeadlineCheckSubscriptionResult {
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
            .build()

        val workflow = client.newWorkflowStub(
            NotificationWorkflow::class.java,
            workflowOptions
        )

       /* val daysToRun = listOf("Mo", "Tu", "We", "Th", "Fr","Sa","Su")
        val timeToRun = "17:12:25+00:00"*/
 /* try {
      workflow.checkUploadAndNotify(
          dataStreamId = dataStreamId,
          dataStreamRoute = dataStreamRoute,
          jurisdiction = jurisdiction,
          daysToRun = daysToRun,
          timeToRun = timeToRun,
          deliveryReference = deliveryReference
      )
  }
  catch (e:Exception){
   val error = e.message
  }*/

        WorkflowClient.start(workflow::checkUploadAndNotify, jurisdiction, dataStreamId, dataStreamRoute, daysToRun, timeToRun, deliveryReference)
        //return true

        return cacheService.updateDeadlineCheckNotificationPreferences(subscription)
    }
}