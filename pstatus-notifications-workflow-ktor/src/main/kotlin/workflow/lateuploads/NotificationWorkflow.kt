package gov.cdc.ocio.processingnotifications.workflow.lateuploads

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

/**
 * The interface which define the upload error and notify method
 */
@WorkflowInterface
interface NotificationWorkflow {

    @WorkflowMethod
    fun checkUploadAndNotify(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        cronSchedule: String,
        emailAddresses: List<String>
    )
}
