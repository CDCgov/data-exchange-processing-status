package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import gov.cdc.ocio.types.model.WorkflowSubscription
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

/**
 * The interface which define the upload error and notify method
 */
@WorkflowInterface
interface DeadlineCheckNotificationWorkflow {

    @WorkflowMethod
    fun checkUploadDeadlinesAndNotify(
        workflowSubscription: WorkflowSubscription
    )
}
