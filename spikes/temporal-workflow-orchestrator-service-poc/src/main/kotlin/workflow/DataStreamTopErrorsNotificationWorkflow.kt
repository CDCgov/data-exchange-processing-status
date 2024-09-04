package gov.cdc.ocio.processingnotifications.workflow

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface DataStreamTopErrorsNotificationWorkflow {

    @WorkflowMethod
    fun checkDataStreamTopErrorsAndNotify(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    )

}
