package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod


/**
 * The interface which defines the digest counts and top errors during an upload and its frequency.
 */
@WorkflowInterface
interface DataStreamTopErrorsNotificationWorkflow {

    @WorkflowMethod
    fun checkDataStreamTopErrorsAndNotify(
        workflowSubscription: WorkflowSubscriptionForDataStreams
    )
}
