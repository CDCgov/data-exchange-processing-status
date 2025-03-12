package gov.cdc.ocio.processingnotifications.workflow

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod


/**
 * The interface which defines the digest counts and top errors during an upload and its frequency.
 */
@WorkflowInterface
interface DataStreamTopErrorsNotificationWorkflow {

    @WorkflowMethod
    fun checkDataStreamTopErrorsAndNotify(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        cronSchedule: String,
        emailAddresses: List<String>
    )
}
