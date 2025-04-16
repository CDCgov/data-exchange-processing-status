package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.processingnotifications.dispatch.Dispatcher
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
        dispatcher: Dispatcher
    )
}
