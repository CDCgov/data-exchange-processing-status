package gov.cdc.ocio.processingnotifications.workflow.lateuploads

import gov.cdc.ocio.types.model.WorkflowSubscription
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

/**
 * The interface which define the upload error and notify method
 */
@WorkflowInterface
interface NotificationWorkflow {

    @WorkflowMethod
    fun checkUploadAndNotify(
        workflowSubscription: WorkflowSubscription
    )
}
