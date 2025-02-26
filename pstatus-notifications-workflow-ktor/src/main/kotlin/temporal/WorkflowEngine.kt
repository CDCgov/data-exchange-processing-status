package gov.cdc.ocio.processingnotifications.temporal

import gov.cdc.ocio.processingnotifications.config.TemporalConfig
import gov.cdc.ocio.processingnotifications.model.getCronExpression
import io.temporal.api.workflow.v1.WorkflowExecutionInfo
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import mu.KotlinLogging


/**
 *  Workflow engine class which creates a grpC client instance of the temporal server
 *  using which it registers the workflow and the activity implementation
 *  Also,using the workflow options the client creates a new workflow stub
 *  Note : CRON expression is used to set the schedule
 */
class WorkflowEngine(temporalConfig: TemporalConfig) {

    private val logger = KotlinLogging.logger {}

    private val target = temporalConfig.temporalServiceTarget

    private val serviceOptions = WorkflowServiceStubsOptions.newBuilder()
        .setTarget(target)
        .build()

    private var service: WorkflowServiceStubs? = null

    private var client: WorkflowClient? = null

    private val healthCheckSystem = HealthCheckTemporalServer(temporalConfig)

    init {
        runCatching {
            service = WorkflowServiceStubs.newServiceStubs(serviceOptions)
            client = WorkflowClient.newInstance(service)
        }
    }

    /**
     * Sets up a temporal workflow.
     *
     * @param taskName String
     * @param daysToRun List<String>
     * @param timeToRun String
     * @param workflowImpl Class<T1>
     * @param activitiesImpl T2
     * @param workflowImplInterface Class<T3>
     * @return T3
     */
    fun <T1 : Any, T2 : Any, T3 : Any> setupWorkflow(
        taskName: String,
        daysToRun: List<String>,
        timeToRun: String,
        workflowImpl: Class<T1>,
        activitiesImpl: T2,
        workflowImplInterface: Class<T3>
    ): T3? {
        try {
            val factory = WorkerFactory.newInstance(client)

            val worker = factory.newWorker(taskName)
            worker.registerWorkflowImplementationTypes(workflowImpl)
            worker.registerActivitiesImplementations(activitiesImpl)
            logger.info("Workflow and Activity successfully registered")
            factory.start()

            val workflowOptions = WorkflowOptions.newBuilder()
                .setTaskQueue(taskName)
                .setCronSchedule(
                    getCronExpression(
                        daysToRun,
                        timeToRun
                    )
                ) // Cron schedule: 15 5 * * 1-5 - Every week day at  5:15a
                .build()

            val workflow = client?.newWorkflowStub(
                workflowImplInterface,
                workflowOptions
            )
            logger.info("Workflow successfully started")
            return workflow
        } catch (ex: Exception) {
            logger.error("Error while creating workflow: ${ex.message}")
        }
        throw Exception("WorkflowEngine instantiation failed. Please try again")
    }

    /**
     * Cancel the workflow based on the workflowId.
     *
     * @param workflowId String
     */
    fun cancelWorkflow(workflowId: String) {
        try {
            // Retrieve the workflow by its ID
            val workflow = client?.newUntypedWorkflowStub(workflowId)

            // Cancel the workflow
            workflow?.cancel()
            logger.info("WorkflowID: $workflowId successfully cancelled")
        } catch (ex: Exception) {
            logger.error("Error while canceling the workflow: ${ex.message}")
        }
        throw Exception("Workflow cancellation failed. Please try again.")
    }

    /**
     * Retrieve only the running workflows.
     *
     * @return List<WorkflowExecutionInfo>
     */
    fun getRunningWorkflows(): List<WorkflowExecutionInfo> {
        return getWorkflows(filterOnlyRunning = true)
    }

    /**
     * Retrieve all the workflows.
     *
     * @return List<WorkflowExecutionInfo>
     */
    fun getAllWorkflows(): List<WorkflowExecutionInfo> {
        return getWorkflows(filterOnlyRunning = false)
    }

    /**
     * Retrieve the workflows, either all or just the ones running.
     *
     * @param filterOnlyRunning Boolean
     * @return List<WorkflowExecutionInfo>
     */
    private fun getWorkflows(filterOnlyRunning: Boolean): List<WorkflowExecutionInfo> {
        val query = when (filterOnlyRunning) {
            true -> "ExecutionStatus='RUNNING'" // Filter for running workflows
            false -> ""
        }
        val request = ListWorkflowExecutionsRequest.newBuilder()
            .setNamespace("default")
            .setQuery(query)
            .build()

        // Fetch workflows
        val response = service?.blockingStub()?.listWorkflowExecutions(request)

        // Log workflow executions
        response?.executionsList?.forEach { execution ->
            logger.info("WorkflowId: ${execution.execution.workflowId}, Type: ${execution.type.name}, Status: ${execution.status}")
        }

        return response?.executionsList?.toList() ?: listOf()
    }

    fun doHealthCheck() = healthCheckSystem.doHealthCheck()
}