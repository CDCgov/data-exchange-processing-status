import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory

fun main() {
    val service = WorkflowServiceStubs.newLocalServiceStubs()
    val client = WorkflowClient.newInstance(service)
    val factory = WorkerFactory.newInstance(client)
    val taskQueue = "notificationTaskQueue"

    val worker = factory.newWorker(taskQueue)
    worker.registerWorkflowImplementationTypes(NotificationWorkflowImpl::class.java)
    worker.registerActivitiesImplementations(NotificationActivitiesImpl())

    factory.start()

    val workflowOptions = WorkflowOptions.newBuilder()
        .setTaskQueue(taskQueue)
        .build()

    val workflow = client.newWorkflowStub(
        NotificationWorkflow::class.java,
        workflowOptions
    )

    val daysToRun = listOf("Mo", "Tu", "We", "Th", "Fr","Sa","Su")
    val timeToRun = "16:11:25+00:00"

    workflow.checkUploadAndNotify(
        dataStreamId = "dataStreamId",
        dataStreamRoute = "dataStreamRoute",
        jurisdiction = "jurisdiction",
        daysToRun = daysToRun,
        timeToRun = timeToRun,
        deliveryReference = "xph6@cdc.gov"
    )
}



