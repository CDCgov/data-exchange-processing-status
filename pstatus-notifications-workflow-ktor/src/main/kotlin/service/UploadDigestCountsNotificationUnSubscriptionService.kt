package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * The main class which unsubscribes the workflow execution for daily upload digest counts.
 *
 * @property logger KLogger
 * @property workflowEngine WorkflowEngine
 */
class UploadDigestCountsNotificationUnSubscriptionService : KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val workflowEngine by inject<WorkflowEngine>()

    /**
     * The main method which cancels a workflow based on the workflow Id
     * @param subscriptionId String
     */
    fun run(subscriptionId: String):
            WorkflowSubscriptionResult {
        try {
            workflowEngine.cancelWorkflow(subscriptionId)
            return WorkflowSubscriptionResult(
                subscriptionId = subscriptionId
            )
        }
        catch (e:Exception ){
            logger.error("Error occurred while checking for upload digest: ${e.message}")
        }
        throw Exception("Error occurred while canceling the execution of workflow engine to cancel workflow for workflow Id $subscriptionId")
    }
}