package gov.cdc.ocio.processingstatusapi.models.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.types.serializers.OffsetDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime


/**
 * Model for the workflow status.
 *
 * @property workflowId String
 * @property runId String
 * @property taskName String
 * @property description String
 * @property status String
 * @property schedule CronSchedule
 * @constructor
 */
@Serializable
@GraphQLDescription("Workflow status describes the scheduled workflows for evaluating conditions to determine if a notification is sent")
data class WorkflowStatus(
    @GraphQLDescription("Workflow ID of the scheduled evaluation workflow")
    val workflowId: String,

    @GraphQLDescription("Run ID of the scheduled evaluation workflow")
    val runId: String,

    @GraphQLDescription("Name of the task to run in the workflow")
    val taskName: String,

    @GraphQLDescription("Description of the scheduled evaluation workflow")
    val description: String,

    @GraphQLDescription("Status of the scheduled evaluation workflow")
    val status: String,

    @GraphQLDescription("Schedule for the workflow to run its evaluation")
    val schedule: CronSchedule,

    @GraphQLDescription("Detailed information regarding a workflow failure, if any")
    val workflowFailureInfo: WorkflowFailureInfo? = null
)

/**
 * Raw cron schedule and its human-readable form.
 *
 * @property cron String?
 * @property description String?
 * @property nextExecution String?
 * @constructor
 */
@Serializable
@GraphQLDescription("Schedule for a workflow to run its evaluation")
data class CronSchedule(
    @GraphQLDescription("Cron unix syntax for the evaluation workflow schedule")
    val cron: String?,

    @GraphQLDescription("Human-readable description of the evaluation workflow schedule")
    val description: String?,

    @GraphQLDescription("Human-readable description of the evaluation workflow schedule")
    @Serializable(with = OffsetDateTimeSerializer::class)
    val lastRun: OffsetDateTime?,

    @GraphQLDescription("Next evaluation workflow execution date/time based on the schedule")
    val nextExecution: String?
)

@Serializable
@GraphQLDescription("Detailed information regarding a workflow failure, if any")
data class WorkflowFailureInfo(

    @GraphQLDescription("Source of the failure")
    val source: String,

    @GraphQLDescription("Failure message")
    val failureMessage: String?,

    @GraphQLDescription("Cause of the failure")
    val causeMessage: String?,

    @GraphQLDescription("Stack trace of the failure")
    val stackTrace: String?,

    @GraphQLDescription("Activity name of the failure")
    val activityName: String?,

    @GraphQLDescription("Retry state of the failure")
    val retryState: String?,

    @GraphQLDescription("Timeout type of the failure")
    val timeoutType: String?
)