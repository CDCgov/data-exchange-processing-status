package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import mu.KotlinLogging


/**
 * The main class which unsubscribes the workflow execution for upload errors.
 *
 * @property cacheService IMemoryCacheService
 * @property workflowEngine WorkflowEngine
 */
class UploadErrorsNotificationUnSubscriptionService {
    private val logger = KotlinLogging.logger {}

    private val workflowEngine: WorkflowEngine = WorkflowEngine()

    /**
     * The main method which cancels a workflow based on the workflow id.
     *
     * @param subscriptionId String
     */
    fun run(subscriptionId: String): WorkflowSubscriptionResult {
        try {
            workflowEngine.cancelWorkflow(subscriptionId)
            return WorkflowSubscriptionResult(
                subscriptionId = subscriptionId,
                message = "",
                deliveryReference = ""
            )
        } catch (e: Exception) {
            logger.error("Error occurred while checking for upload deadline: ${e.message}")
        }
        throw Exception("Error occurred while canceling the execution of workflow engine to cancel workflow for workflow Id $subscriptionId")
    }
}
