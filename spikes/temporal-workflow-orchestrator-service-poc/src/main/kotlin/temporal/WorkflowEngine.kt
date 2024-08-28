package gov.cdc.ocio.processingnotifications.temporal

import gov.cdc.ocio.processingnotifications.model.getCronExpression
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.client.WorkflowStub
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory

class WorkflowEngine {

    fun<T1 :Any,T2 : Any, T3: Any>  setupWorkflow(
        taskName:String,
        daysToRun:List<String>, timeToRun:String,
        workflowImpl: Class<T1>, activitiesImpl:T2, workflowImplInterface:Class<T3>):T3{

        val service = WorkflowServiceStubs.newLocalServiceStubs()
        val client = WorkflowClient.newInstance(service)
        val factory = WorkerFactory.newInstance(client)

        val worker = factory.newWorker(taskName)
        worker.registerWorkflowImplementationTypes(workflowImpl)
        worker.registerActivitiesImplementations(activitiesImpl)

        factory.start()

        val workflowOptions = WorkflowOptions.newBuilder()
            .setTaskQueue(taskName)
            .setCronSchedule(getCronExpression(daysToRun,timeToRun)) // Cron schedule: 15 5 * * 1-5 - Every week day at  5:15a
            .build()

        val workflow = client.newWorkflowStub(
            workflowImplInterface,
            workflowOptions
        )
        return workflow
    }

    fun cancelWorkflow(workflowId:String){
        val service = WorkflowServiceStubs.newLocalServiceStubs()
        val client = WorkflowClient.newInstance(service)

        // Retrieve the workflow by its ID
        val workflow: WorkflowStub = client.newUntypedWorkflowStub(workflowId)
        // Cancel the workflow
        workflow.cancel()
    }
}