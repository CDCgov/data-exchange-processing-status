package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import mu.KotlinLogging

/**
 * The main class which subscribes the workflow execution
 * for digest counts and top errors and its frequency for each upload
 * @property cacheService IMemoryCacheService
 * @property workflowEngine WorkflowEngine

 */
class DataStreamTopErrorsNotificationUnSubscriptionService {
    private val logger = KotlinLogging.logger {}
    private val workflowEngine: WorkflowEngine = WorkflowEngine()

    /**
     * The main function which is used to cancel the workflow based on the workflowID
     * @param subscriptionId String
     * @return WorkflowSubscriptionResult
     */
    fun run(subscriptionId: String):
            WorkflowSubscriptionResult {
        try {
            workflowEngine.cancelWorkflow(subscriptionId)
            return WorkflowSubscriptionResult(
                subscriptionId = subscriptionId,
                message = "",
                deliveryReference = ""
            )
        }
        catch (e:Exception){
            logger.error("Error occurred while unsubscribing and canceling the workflow for digest counts and top errors with workflowId $subscriptionId: ${e.message}")
        }
        throw Exception("Error occurred while canceling the workflow engine for digest counts and top for workflow Id $subscriptionId")
    }
}
