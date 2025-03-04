package gov.cdc.ocio.processingstatusapi.models.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.types.serializers.OffsetDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime


/**
 * Model for the workflow status.
 *
 * @property workflowId String
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

    @GraphQLDescription("Name of the task to run in the workflow")
    val taskName: String,

    @GraphQLDescription("Description of the scheduled evaluation workflow")
    val description: String,

    @GraphQLDescription("Status of the scheduled evaluation workflow")
    val status: String,

    @GraphQLDescription("Schedule for the workflow to run its evaluation")
    val schedule: CronSchedule
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