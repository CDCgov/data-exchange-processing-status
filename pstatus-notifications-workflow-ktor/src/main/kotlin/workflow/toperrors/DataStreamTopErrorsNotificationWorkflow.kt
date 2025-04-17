package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.processingnotifications.model.WorkflowSubscription
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod


/**
 * The interface which defines the digest counts and top errors during an upload and its frequency.
 */
@WorkflowInterface
interface DataStreamTopErrorsNotificationWorkflow {

    @WorkflowMethod
    fun checkDataStreamTopErrorsAndNotify(
        workflowSubscription: WorkflowSubscription
    )
}
