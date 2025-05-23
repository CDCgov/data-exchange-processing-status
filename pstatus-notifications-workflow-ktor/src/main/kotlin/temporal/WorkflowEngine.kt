package gov.cdc.ocio.processingnotifications.temporal

import com.fasterxml.jackson.databind.DeserializationFeature
import com.google.protobuf.ByteString
import gov.cdc.ocio.processingnotifications.config.TemporalConfig
import gov.cdc.ocio.processingnotifications.model.CronSchedule
import gov.cdc.ocio.processingnotifications.model.WorkflowStatus
import gov.cdc.ocio.processingnotifications.utils.CronUtils
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.temporal.api.enums.v1.TaskQueueKind
import io.temporal.api.enums.v1.WorkflowExecutionStatus
import io.temporal.api.workflow.v1.WorkflowExecutionInfo
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest
import io.temporal.api.workflowservice.v1.DescribeTaskQueueRequest
import io.temporal.api.taskqueue.v1.TaskQueue
import io.temporal.api.enums.v1.TaskQueueType
import io.temporal.api.workflowservice.v1.DescribeTaskQueueResponse
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import mu.KotlinLogging
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckNotificationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckNotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.toperrors.TopErrorsNotificationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.toperrors.TopErrorsNotificationActivitiesImpl
import io.temporal.client.WorkflowNotFoundException
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter


/**
 * WorkflowEngine is responsible for setting up, managing, and monitoring workflows using
 * Temporal framework. It provides functionalities to initialize Temporal clients,
 * schedule workflows, manage workflow execution, monitor worker status, and retrieve
 * workflow information.
 *
 * @constructor Creates a new instance of WorkflowEngine with the specified Temporal configuration.
 * @param temporalConfig The Temporal configuration required to set up the client and server connection.
 */
class WorkflowEngine(
    private val temporalConfig: TemporalConfig
) {

    private val logger = KotlinLogging.logger {}

    private val jacksonObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val payloadConverter = JacksonJsonPayloadConverter(jacksonObjectMapper)

    private val dataConverter = DefaultDataConverter(payloadConverter)

    private val serviceOptions = WorkflowServiceStubsOptions.newBuilder()
        .setTarget(temporalConfig.serviceTarget)
        .build()

    private val clientOptions = WorkflowClientOptions.newBuilder()
        .setNamespace(temporalConfig.namespace)
        .setDataConverter(dataConverter)
        .build()

    private lateinit var service: WorkflowServiceStubs

    private lateinit var client: WorkflowClient

    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private val healthCheckSystem = HealthCheckTemporalServer(temporalConfig)

    /**
     * A map that associates workflow implementation classes to their corresponding activities implementation instances.
     *
     * This map is used to pair specific workflow implementations with their designated activity implementations.
     * Each key in the map represents a workflow implementation class, while the corresponding value is an instance
     * of the activities class that performs the required tasks for the workflow.
     *
     * The map is utilized within the `WorkflowEngine` class to dynamically configure and manage workflows alongside
     * their associated activities during runtime.
     */
    private val workflowToActivitiesMap = mapOf(
        DeadlineCheckNotificationWorkflowImpl::class.java to DeadlineCheckNotificationActivitiesImpl(),
        UploadDigestCountsNotificationWorkflowImpl::class.java to UploadDigestCountsNotificationActivitiesImpl(),
        TopErrorsNotificationWorkflowImpl::class.java to TopErrorsNotificationActivitiesImpl()
    )

    init {
        initializeTemporalClient()
        startWorkerMonitor()
    }

    private fun initializeTemporalClient() {
        runCatching {
            service = WorkflowServiceStubs.newServiceStubs(serviceOptions)
            client = WorkflowClient.newInstance(service, clientOptions)
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

        val factory = WorkerFactory.newInstance(client)
        val worker = factory.newWorker(taskQueue)
        worker?.let {
            it.registerWorkflowImplementationTypes(workflowImpl)
            it.registerActivitiesImplementations(activitiesImpl)
            logger.info("Workflow and Activity registered for task queue: $taskQueue")
        } ?: error("Failed to create a worker for task queue: $taskQueue")

        logger.info("Workflow and Activity successfully registered")

        // Start the factory after registering the worker
        factory.start()
        logger.info("Worker factory started")

        return workflow
    }

    /**
     * Cancel the workflow based on the workflowId.
     *
     * @param workflowId String
     */
    @Throws(WorkflowNotFoundException::class)
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
            throw ex
        }
    }

    /**
     * Retrieve only the running workflows.
     *
     * @return List<WorkflowStatus>
     */
    private fun getRunningWorkflows(): List<WorkflowStatus> {
        return getWorkflows(filterOnlyRunning = true, includeWorkerCheck = true)
    }

    /**
     * Retrieve all the workflows.
     *
     * @return List<WorkflowStatus>
     */
    fun getAllWorkflows(): List<WorkflowStatus> {
        return getWorkflows(filterOnlyRunning = false, includeWorkerCheck = false)
    }

    /**
     * Periodically checks if the workers are running, and restarts them if necessary.
     */
    private fun startWorkerMonitor() {
        scheduler.scheduleAtFixedRate({
            runCatching {
                checkWorkersAttached()
            }.onFailure {
                logger.error("Error in worker monitoring: ${it.message}")
            }
        }, 10, 30, TimeUnit.SECONDS)
    }

    /**
     * Restart all registered workers.
     */
    private fun checkWorkersAttached() {
        val runningWorkflows = getRunningWorkflows()
        runningWorkflows.forEach { workflow ->
            if (workflow.workerAttached == false) {
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
     * Retrieve the workflows, either all or just the ones running.
     *
     * @param filterOnlyRunning Boolean
     * @return List<WorkflowStatus>
     */
    private fun getWorkflows(
        filterOnlyRunning: Boolean,
        includeWorkerCheck: Boolean = false
    ): List<WorkflowStatus> {
        val query = when (filterOnlyRunning) {
            true -> "ExecutionStatus = 'Running'" // Filter for running workflows
            false -> ""
        }
        val pageSize = 50
        var nextPageToken: ByteString? = null
        val workflows = mutableListOf<WorkflowExecutionInfo>()
        do {
            val requestBuilder = ListWorkflowExecutionsRequest.newBuilder()
                .setNamespace(temporalConfig.namespace)
                .setPageSize(pageSize)
                .setQuery(query)

            nextPageToken?.let {
                requestBuilder.setNextPageToken(it)
            }

            // Fetch workflows
            val response = service.blockingStub()?.listWorkflowExecutions(requestBuilder.build())
            response?.executionsList?.let { workflows.addAll(it) }

            nextPageToken = response?.takeIf { !it.nextPageToken.isEmpty }?.nextPageToken

        } while (nextPageToken != null)

        val results = workflows.map { executionInfo ->
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
            val workerAttached = if (includeWorkerCheck) workerHasPoller(executionInfo.taskQueue) else null

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

        return results
    }

    /**
     * Determines if the task queue provided has an active poller.  If it doesn't, it means there is no worker
     * attached.
     *
     * @param taskQueue String
     * @return Boolean - Returns true if a worker is attached to this queue, false otherwise.
     */
    private fun workerHasPoller(taskQueue: String): Boolean {
        val describeTaskQueueResponse = runBlocking { describeTaskQueueWithRetry(taskQueue) }
        val pollers = describeTaskQueueResponse.pollersList
        logger.info("Number of active workers: ${pollers.size}")

        if (pollers.isEmpty()) {
            logger.info("No workers are currently polling this task queue!")
        }
        return pollers.isNotEmpty()
    }

    /**
     * Describe the task queue with retries since rapid calls to this can trigger a Temporal retry limit.
     * The "suspend" in the function signature is needed for the coroutine delay call if backoff is needed.
     *
     * @param taskQueue String
     * @return DescribeTaskQueueResponse
     */
    private suspend fun describeTaskQueueWithRetry(taskQueue: String): DescribeTaskQueueResponse {
        val maxRetries = 5
        var attempt = 0
        var backoffMillis = 500L

        while (attempt < maxRetries) {
            try {
                val request = DescribeTaskQueueRequest.newBuilder()
                    .setNamespace(temporalConfig.namespace)
                    .setTaskQueue(
                        TaskQueue.newBuilder()
                            .setName(taskQueue)
                            .setKind(TaskQueueKind.TASK_QUEUE_KIND_NORMAL)
                    )
                    .setTaskQueueType(TaskQueueType.TASK_QUEUE_TYPE_WORKFLOW)
                    .build()

                return service.blockingStub().describeTaskQueue(request)
            } catch (e: StatusRuntimeException) {
                if (e.status.code == Status.Code.RESOURCE_EXHAUSTED) {
                    logger.warn("Temporal rate limited. Retrying in ${backoffMillis}ms...")
                    delay(backoffMillis)
                    backoffMillis *= 2 // exponential backoff
                    attempt++
                } else {
                    throw e // rethrow if it's not a rate limit issue
                }
            }
        }

        throw RuntimeException("Exceeded retry attempts due to rate limiting.")
    }

    /**
     * Restart a worker with the provided task queue and workflow implementation class.
     *
     * @param taskQueue String
     * @param workflowImpl Class<*>
     */
    private fun restartWorker(taskQueue: String, workflowImpl: Class<*>) {
        // Create a Worker Factory
        val factory = WorkerFactory.newInstance(client)

        // Register a worker on the same task queue
        val worker = factory.newWorker(taskQueue)

        // Register workflow and activities
        worker.registerWorkflowImplementationTypes(workflowImpl)
        worker.registerActivitiesImplementations(workflowToActivitiesMap[workflowImpl])
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