package gov.cdc.ocio.processingnotifications.workflow

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

/**
 * Interface that defines the upload errors and notify
 */
@WorkflowInterface
interface UploadErrorsNotificationWorkflow {

    @WorkflowMethod
    fun checkUploadErrorsAndNotify(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    )

}
