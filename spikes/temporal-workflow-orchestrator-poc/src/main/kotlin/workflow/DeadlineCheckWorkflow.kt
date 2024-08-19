import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface DeadlineCheckWorkflow {
    @WorkflowMethod
    fun execute(
        jurisdiction: String,
        dataStreamId: String,
        dataStreamRoute: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    )
}
