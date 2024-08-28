package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine

class DeadLineCheckUnSubscriptionService {
    private val cacheService: InMemoryCacheService = InMemoryCacheService()
    private val workflowEngine: WorkflowEngine = WorkflowEngine()
    fun run(subscriptionId: String):
            WorkflowSubscriptionResult {
      workflowEngine.cancelWorkflow(subscriptionId)
      return cacheService.updateDeadlineCheckUnSubscriptionPreferences(subscriptionId)
    }
}
