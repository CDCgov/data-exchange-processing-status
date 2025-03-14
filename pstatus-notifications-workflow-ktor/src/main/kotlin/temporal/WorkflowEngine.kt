package gov.cdc.ocio.processingnotifications.temporal

import gov.cdc.ocio.processingnotifications.config.TemporalConfig
import gov.cdc.ocio.processingnotifications.model.CronSchedule
import gov.cdc.ocio.processingnotifications.model.WorkflowStatus
import gov.cdc.ocio.processingnotifications.utils.CronUtils
import io.temporal.api.enums.v1.WorkflowExecutionStatus
import io.temporal.api.workflow.v1.WorkflowExecutionInfo
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory
import mu.KotlinLogging
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


/**
 *  Workflow engine class which creates a grpC client instance of the temporal server
 *  using which it registers the workflow and the activity implementation
 *  Also,using the workflow options the client creates a new workflow stub
 *  Note : CRON expression is used to set the schedule
 */
class WorkflowEngine(private val temporalConfig: TemporalConfig) {

    private val logger = KotlinLogging.logger {}

    private val serviceOptions = WorkflowServiceStubsOptions.newBuilder()
        .setTarget(temporalConfig.serviceTarget)
        .build()

    private var service: WorkflowServiceStubs? = null

    private var client: WorkflowClient? = null

    private val healthCheckSystem = HealthCheckTemporalServer(temporalConfig)

    private var factory: WorkerFactory? = null

    private val workers = mutableMapOf<String, Worker>()

    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    init {
        initializeTemporalClient()
        startWorkerMonitor()
    }

    private fun initializeTemporalClient() {
        runCatching {
            service = WorkflowServiceStubs.newServiceStubs(serviceOptions)
            client = WorkflowClient.newInstance(service)
            factory = WorkerFactory.newInstance(client)
        }.onFailure { ex ->
            logger.error("Failed to initialize Temporal client: ${ex.message}")
        }
    }

    /**
     * Sets up a temporal workflow.
     *
     * @param description String
     * @param taskName String
     * @param cronSchedule String
     * @param workflowImpl Class<T1>
     * @param activitiesImpl T2
     * @param workflowImplInterface Class<T3>
     * @return T3?
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class)
    fun <T1 : Any, T2 : Any, T3 : Any> setupWorkflow(
        description: String,
        taskName: String,
        cronSchedule: String,
        workflowImpl: Class<T1>,
        activitiesImpl: T2,
        workflowImplInterface: Class<T3>
    ): T3 {
        CronUtils.checkValid(cronSchedule)

        client ?: throw IllegalArgumentException("Workflow client is not established")
        factory ?: throw IllegalArgumentException("Worker factory is not initialized")

        val worker = factory?.newWorker(taskName)
        if (worker != null) {
            worker.registerWorkflowImplementationTypes(workflowImpl)
            worker.registerActivitiesImplementations(activitiesImpl)
            workers[taskName] = worker
            logger.info("Workflow and Activity registered for task queue: $taskName")
        } else {
            throw IllegalStateException("Failed to create a worker for task queue: $taskName")
        }

        logger.info("Workflow and Activity successfully registered")
        if (!factory!!.isStarted) {
            factory!!.start()
            logger.info("Worker factory started")
        }

        val workflowOptions = WorkflowOptions.newBuilder()
            .setTaskQueue(taskName)
            .setMemo(mapOf("description" to description))
            .setCronSchedule(cronSchedule) // Cron schedule: 15 5 * * 1-5 - Every week day at 5:15a
            .build()

        val workflow = client!!.newWorkflowStub(
            workflowImplInterface,
            workflowOptions
        )
        logger.info("Workflow successfully started")
        return workflow
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
            val message = "Error while canceling the workflow: ${ex.message}"
            logger.error(message)
            throw Exception(message)
        }
    }

    /**
     * Retrieve only the running workflows.
     *
     * @return List<WorkflowStatus>
     */
    fun getRunningWorkflows(): List<WorkflowStatus> {
        return getWorkflows(filterOnlyRunning = true)
    }

    /**
     * Retrieve all the workflows.
     *
     * @return List<WorkflowStatus>
     */
    fun getAllWorkflows(): List<WorkflowStatus> {
        return getWorkflows(filterOnlyRunning = false)
    }

    /**
     * Periodically checks if the workers are running, and restarts them if necessary.
     */
    private fun startWorkerMonitor() {
        scheduler.scheduleAtFixedRate({
            repeat(workers.keys.size) {
                try {
                    if (factory == null || !factory!!.isStarted) {
                        logger.warn("Worker factory is not running. Restarting workers...")
                        restartWorkers()
                    }
                } catch (e: Exception) {
                    logger.error("Error in worker monitoring: ${e.message}")
                }
            }
        }, 10, 30, TimeUnit.SECONDS)
    }

    /**
     * Restart all registered workers.
     */
    private fun restartWorkers() {
        initializeTemporalClient()
        workers.forEach { (taskName, _) ->
            factory!!.newWorker(taskName)
            logger.info("Restarting worker for task queue: $taskName")
        }

        factory!!.start()
        logger.info("Worker factory restarted")
    }

    /**
     * Shutdown workers gracefully.
     */
    fun shutdown() {
        logger.info("Shutting down Temporal workers...")
        factory?.shutdown()
        service?.shutdown()
        scheduler.shutdown()
    }

    /**
     * Retrieve the workflows, either all or just the ones running.
     *
     * @param filterOnlyRunning Boolean
     * @return List<WorkflowStatus>
     */
    private fun getWorkflows(filterOnlyRunning: Boolean): List<WorkflowStatus> {
        val query = when (filterOnlyRunning) {
            true -> "ExecutionStatus='RUNNING'" // Filter for running workflows
            false -> ""
        }
        val request = ListWorkflowExecutionsRequest.newBuilder()
            .setNamespace(temporalConfig.namespace)
            .setQuery(query)
            .build()

        // Fetch workflows
        val response = service?.blockingStub()?.listWorkflowExecutions(request)

        val results = response?.executionsList?.map { executionInfo ->
            // Log workflow executions
            logger.info("WorkflowId: ${executionInfo.execution.workflowId}, Type: ${executionInfo.type.name}, Status: ${executionInfo.status}")

            val cronScheduleRaw = getWorkflowCronSchedule(executionInfo)
            val cronScheduleDescription = runCatching {
                CronUtils.description(cronScheduleRaw)
            }
            val nextExecution = runCatching { CronUtils.nextExecution(cronScheduleRaw) }.getOrNull()
            val taskName = executionInfo.type.name
            val ts = executionInfo.executionTime
            val lastRun = OffsetDateTime.ofInstant(Instant.ofEpochSecond(ts.seconds, ts.nanos.toLong()), ZoneOffset.UTC)
            val descPayload = runCatching { executionInfo.memo.getFieldsOrThrow("description") }
            val description = descPayload.getOrNull()?.data?.toStringUtf8()?.replace("\"", "") ?: "unknown"

            val cronSchedule = CronSchedule(
                cron = cronScheduleRaw,
                description = cronScheduleDescription.getOrDefault(
                    "Parse Error: ${cronScheduleDescription.exceptionOrNull()?.localizedMessage ?: "unknown error"}"
                ),
                lastRun,
                nextExecution = nextExecution?.toString()
            )
            WorkflowStatus(
                executionInfo.execution.workflowId,
                taskName,
                description,
                executionInfo.status.name,
                cronSchedule
            )
        }

        return results ?: listOf()
    }

    /**
     * Get the raw cron schedule string for a given workflow execution info.
     *
     * @param wfExecInfo WorkflowExecutionInfo
     * @return String?
     */
    private fun getWorkflowCronSchedule(wfExecInfo: WorkflowExecutionInfo): String? {
        if (wfExecInfo.status != WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING)
            return null

        val req = GetWorkflowExecutionHistoryRequest.newBuilder()
            .setNamespace(client?.options?.namespace)
            .setExecution(wfExecInfo.execution)
            .build()

        val res = service?.blockingStub()?.getWorkflowExecutionHistory(req)

        val firstHistoryEvent = res?.history?.eventsList?.get(0)

        return firstHistoryEvent?.workflowExecutionStartedEventAttributes?.cronSchedule
    }

    fun doHealthCheck() = healthCheckSystem.doHealthCheck()
}