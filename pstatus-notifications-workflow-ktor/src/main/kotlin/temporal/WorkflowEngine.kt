package gov.cdc.ocio.processingnotifications.temporal

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.config.TemporalConfig
import gov.cdc.ocio.processingnotifications.model.CronSchedule
import gov.cdc.ocio.processingnotifications.model.WorkflowStatus
import gov.cdc.ocio.processingnotifications.utils.CronUtils
import io.temporal.api.enums.v1.WorkflowExecutionStatus
import io.temporal.api.workflow.v1.WorkflowExecutionInfo
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest
import io.temporal.api.workflowservice.v1.DescribeTaskQueueRequest
import io.temporal.api.taskqueue.v1.TaskQueue
import io.temporal.api.enums.v1.TaskQueueType
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass


/**
 * Workflow engine class which creates a grpC client instance of the temporal server
 * using which it registers the workflow and the activity implementation
 * Also,using the workflow options the client creates a new workflow stub
 * Note: CRON expression is used to set the schedule
 *
 * @property temporalConfig TemporalConfig
 * @property logger KLogger
 * @property serviceOptions (WorkflowServiceStubsOptions..WorkflowServiceStubsOptions?)
 * @property service WorkflowServiceStubs
 * @property client WorkflowClient
 * @property factory WorkerFactory
 * @property workers MutableMap<String, Worker>
 * @property scheduler [@EnhancedForWarnings(ScheduledExecutorService)] (ScheduledExecutorService..ScheduledExecutorService?)
 * @property healthCheckSystem HealthCheckTemporalServer
 * @constructor
 */
class WorkflowEngine(
    private val temporalConfig: TemporalConfig
) {

    private val logger = KotlinLogging.logger {}

    private val serviceOptions = WorkflowServiceStubsOptions.newBuilder()
        .setTarget(temporalConfig.serviceTarget)
        .build()

    private val clientOptions = WorkflowClientOptions.newBuilder()
        .setNamespace(temporalConfig.namespace)
        .build()

    private lateinit var service: WorkflowServiceStubs

    private lateinit var client: WorkflowClient

    private lateinit var factory: WorkerFactory

    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private val healthCheckSystem = HealthCheckTemporalServer(temporalConfig)

    init {
        initializeTemporalClient()
        startWorkerMonitor()
    }

    private fun initializeTemporalClient() {
        runCatching {
            service = WorkflowServiceStubs.newServiceStubs(serviceOptions)
            client = WorkflowClient.newInstance(service, clientOptions)
            factory = WorkerFactory.newInstance(client)
        }.onFailure { ex ->
            logger.error("Failed to initialize Temporal client: ${ex.message}")
        }
    }

    /**
     * Sets up a temporal workflow.
     *
     * @param description String
     * @param taskQueue String
     * @param cronSchedule String
     * @param workflowImpl Class<T1>
     * @param activitiesImpl T2
     * @param workflowImplInterface Class<T3>
     * @return T3?
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class)
    fun <T1 : Any, T2 : Any, T3 : Any> setupWorkflow(
        description: String,
        taskQueue: String,
        cronSchedule: String,
        workflowImpl: Class<T1>,
        activitiesImpl: T2,
        workflowImplInterface: Class<T3>
    ): T3 {
        CronUtils.checkValid(cronSchedule)

        val worker = factory.newWorker(taskQueue)
        worker?.let {
            it.registerWorkflowImplementationTypes(workflowImpl)
            it.registerActivitiesImplementations(activitiesImpl)
            logger.info("Workflow and Activity registered for task queue: $taskQueue")
        } ?: error("Failed to create a worker for task queue: $taskQueue")

        logger.info("Workflow and Activity successfully registered")
        if (!factory.isStarted) {
            factory.start()
            logger.info("Worker factory started")
        }

        val workflowOptions = WorkflowOptions.newBuilder()
            .setTaskQueue(taskQueue)
            .setMemo(
                mapOf(
                    "description" to description,
                    "workflowImplClassName" to workflowImpl.name
                )
            )
            .setCronSchedule(cronSchedule) // Cron schedule: 15 5 * * 1-5 - Every week day at 5:15a
            .build()

        val workflow = client.newWorkflowStub(
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
            val workflow = client.newUntypedWorkflowStub(workflowId)

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
            try {
//                if (!factory.isStarted) {
//                    logger.info("Worker factory is not running. Restarting...")
//                    factory.start()
//                }
                checkWorkersAttached()
            } catch (e: Exception) {
                logger.error("Error in worker monitoring: ${e.message}")
            }
        }, 10, 30, TimeUnit.SECONDS)
    }

    /**
     * Restart all registered workers.
     */
    private fun checkWorkersAttached() {
        val runningWorkflows = getRunningWorkflows()
        runningWorkflows.forEach { workflow ->
            if (!workflow.workerAttached) {
                val taskQueue = workflow.taskQueue
                logger.warn("Restarting worker for task queue: $taskQueue")
                val workflowImplClassName = workflow.workflowImplClassName
                workflowImplClassName ?: error("Unknown workflow implementation, can't restart worker")
                val workflowImpl = Class.forName(workflowImplClassName)
                restartWorker(taskQueue, workflowImpl)
            }
        }
    }

    /**
     * Shutdown workers gracefully.
     */
    fun shutdown() {
        logger.info("Shutting down Temporal workers...")
        factory.shutdown()
        service.shutdown()
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
            true -> "ExecutionStatus = 'Running'" // Filter for running workflows
            false -> ""
        }
        val request = ListWorkflowExecutionsRequest.newBuilder()
            .setNamespace(temporalConfig.namespace)
            .setQuery(query)
            .build()

        // Fetch workflows
        val response = service.blockingStub()?.listWorkflowExecutions(request)

        val results = response?.executionsList?.map { executionInfo ->
            // Log workflow executions
            logger.info("WorkflowId: ${executionInfo.execution.workflowId}, Type: ${executionInfo.type.name}, Status: ${executionInfo.status}")

            val cronScheduleRaw = getWorkflowCronSchedule(executionInfo)
            val cronScheduleDescription = runCatching {
                CronUtils.description(cronScheduleRaw)
            }
            val nextExecution = runCatching { CronUtils.nextExecution(cronScheduleRaw) }.getOrNull()
            val taskName = executionInfo.type.name
            val taskQueue = executionInfo.taskQueue
            val ts = executionInfo.executionTime
            val lastRun = OffsetDateTime.ofInstant(Instant.ofEpochSecond(ts.seconds, ts.nanos.toLong()), ZoneOffset.UTC)
            val descPayload = runCatching { executionInfo.memo.getFieldsOrThrow("description") }
            val description = descPayload.getOrNull()?.data?.toStringUtf8()?.replace("\"", "") ?: "unknown"
            val workflowImplClassNamePayload = runCatching { executionInfo.memo.getFieldsOrThrow("workflowImplClassName") }
            val workflowImplClassName = workflowImplClassNamePayload.getOrNull()?.data?.toStringUtf8()?.replace("\"", "")
            val workerAttached = workerHasPoller(executionInfo.taskQueue)

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
                taskQueue,
                description,
                workerAttached,
                executionInfo.status.name,
                cronSchedule,
                workflowImplClassName
            )
        }

        return results ?: listOf()
    }

    private fun workerHasPoller(taskQueue: String): Boolean {
        val describeTaskQueueResponse = service.blockingStub()
            .describeTaskQueue(
                DescribeTaskQueueRequest.newBuilder()
                    .setNamespace(temporalConfig.namespace)
                    .setTaskQueue(
                        TaskQueue.newBuilder().setName(taskQueue).build()
                    )
                    .setTaskQueueType(TaskQueueType.TASK_QUEUE_TYPE_WORKFLOW)
                    .build()
            )

        val pollers = describeTaskQueueResponse.pollersList
        logger.info("Number of active workers: ${pollers.size}")

        if (pollers.isEmpty()) {
            logger.info("No workers are currently polling this task queue!")
        }
        return pollers.isNotEmpty()
    }

    private fun restartWorker(taskQueue: String, workflowImpl: Class<*>) {
        // Create a Worker Factory
        val factory = WorkerFactory.newInstance(client)

        // Register a worker on the same task queue
        val worker = factory.newWorker(taskQueue)

        // Register workflow and activities
        worker.registerWorkflowImplementationTypes(workflowImpl)
        worker.registerActivitiesImplementations(NotificationActivitiesImpl())

        // Start the worker
        factory.start()

        logger.info("Worker started and polling for tasks...")
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
            .setNamespace(client.options?.namespace)
            .setExecution(wfExecInfo.execution)
            .build()

        runCatching {
            service.blockingStub()?.getWorkflowExecutionHistory(req)
        }.onSuccess {
            val firstHistoryEvent = it?.history?.eventsList?.get(0)
            return firstHistoryEvent?.workflowExecutionStartedEventAttributes?.cronSchedule
        }
        return null
    }

    fun doHealthCheck() = healthCheckSystem.doHealthCheck()
}