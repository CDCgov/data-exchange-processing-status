package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import io.temporal.client.WorkflowNotFoundException
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * The main class which subscribes the workflow execution for digest counts and top errors and its frequency for
 * each upload.
 *
 * @property logger KLogger
 * @property workflowEngine WorkflowEngine
 */
class DataStreamTopErrorsNotificationUnSubscriptionService : KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val workflowEngine by inject<WorkflowEngine>()

    /**
     * The main function which is used to cancel the workflow based on the workflowID.
     *
     * @param subscriptionId String
     * @return WorkflowSubscriptionResult
     */
    fun run(subscriptionId: String): WorkflowSubscriptionResult {
        try {
            workflowEngine.cancelWorkflow(subscriptionId)
            return WorkflowSubscriptionResult(
                subscriptionId = subscriptionId
            )
        } catch (ex: Exception) {
            logger.error("Error occurred while unsubscribing and canceling the workflow for workflowId $subscriptionId: ${ex.message}")
            throw ex
        }
    }
}
