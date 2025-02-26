package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import mu.KotlinLogging


/**
 * The main class which unsubscribes the workflow execution
 * for upload errors.
 *
 * @property logger KLogger
 * @property workflowEngine WorkflowEngine
 */
class DeadLineCheckUnSubscriptionService {
    private val logger = KotlinLogging.logger {}
    private val workflowEngine: WorkflowEngine = WorkflowEngine()

    /**
     * The main method which cancels the workflow based on the workflow Id
     * @param subscriptionId String
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
            logger.error("Error occurred while unsubscribing and canceling the workflow for upload deadline with workflowId $subscriptionId: ${e.message}")
        }
        throw Exception("Error occurred while canceling the workflow execution engine for upload deadline check for workflow Id $subscriptionId")
    }
}
