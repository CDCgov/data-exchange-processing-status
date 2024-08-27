package gov.cdc.ocio.processingnotifications.service


import gov.cdc.ocio.processingnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowStub
import io.temporal.serviceclient.WorkflowServiceStubs


class UploadErrorsNotificationUnSubscriptionService {
    private val cacheService: InMemoryCacheService = InMemoryCacheService()
    fun run(subscriptionId: String):
            WorkflowSubscriptionResult {
        val service = WorkflowServiceStubs.newLocalServiceStubs()
        val client = WorkflowClient.newInstance(service)

        // Retrieve the workflow by its ID
        val workflow: WorkflowStub = client.newUntypedWorkflowStub(subscriptionId)
        // Cancel the workflow
        workflow.cancel()
        return cacheService.updateDeadlineCheckUnSubscriptionPreferences(subscriptionId)
    }
}
