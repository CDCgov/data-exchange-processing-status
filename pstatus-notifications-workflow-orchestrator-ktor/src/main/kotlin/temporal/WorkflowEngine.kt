package gov.cdc.ocio.processingnotifications.temporal

import gov.cdc.ocio.processingnotifications.model.getCronExpression
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.client.WorkflowStub
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory
import mu.KotlinLogging

/**
 *  Workflow engine class which creates a grpC client instance of the temporal server
 *  using which it registers the workflow and the activity implementation
 *  Also,using the workflow options the client creates a new workflow stub
 *  Note : CRON expression is used to set the schedule
 */
class WorkflowEngine {
    private val logger = KotlinLogging.logger {}

    fun<T1 :Any,T2 : Any, T3: Any>  setupWorkflow(
        taskName:String,
        daysToRun:List<String>, timeToRun:String,
        workflowImpl: Class<T1>, activitiesImpl:T2, workflowImplInterface:Class<T3>):T3{
        try {
            val service = WorkflowServiceStubs.newLocalServiceStubs()
            val client = WorkflowClient.newInstance(service)
            val factory = WorkerFactory.newInstance(client)

            val worker = factory.newWorker(taskName)
            worker.registerWorkflowImplementationTypes(workflowImpl)
            worker.registerActivitiesImplementations(activitiesImpl)
            logger.info("Workflow and Activity successfully registered")
            factory.start()

            val workflowOptions = WorkflowOptions.newBuilder()
                .setTaskQueue(taskName)
                .setCronSchedule(getCronExpression(daysToRun,timeToRun)) // Cron schedule: 15 5 * * 1-5 - Every week day at  5:15a
                .build()

            val workflow = client.newWorkflowStub(
                workflowImplInterface,
                workflowOptions
            )
            logger.info("Workflow successfully started")
            return workflow
        }
        catch (ex: Exception){
            logger.error("Error while creating workflow: ${ex.message}")
        }
        throw Exception("WorkflowEngine instantiation failed. Please try again")
    }

    /**
     * Cancel the workflow based on the workflowId
     * @param workflowId String
     */
    fun cancelWorkflow(workflowId:String){
        try {
            val service = WorkflowServiceStubs.newLocalServiceStubs()
            val client = WorkflowClient.newInstance(service)

            // Retrieve the workflow by its ID
            val workflow: WorkflowStub = client.newUntypedWorkflowStub(workflowId)
            // Cancel the workflow
            workflow.cancel()
            logger.info("WorkflowID:$workflowId successfully cancelled")
        }
        catch (ex: Exception){
            logger.error("Error while canceling the workflow : ${ex.message}")
        }
        throw Exception("Workflow cancellation failed. Please try again")

    }
}