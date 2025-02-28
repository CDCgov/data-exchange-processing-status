package gov.cdc.ocio.processingstatusapi.models.query

import kotlinx.serialization.Serializable


/**
 * Model for the workflow status.
 *
 * @property workflowId String
 * @property description String
 * @property status String
 * @property schedule CronSchedule
 * @constructor
 */
@Serializable
data class WorkflowStatus(
    val workflowId: String,
    val description: String,
    val status: String,
    val schedule: CronSchedule
)

/**
 * Raw cron schedule and its human-readable form.
 *
 * @property cron String
 * @property description String
 * @property nextExecution String
 * @constructor
 */
@Serializable
data class CronSchedule(
    val cron: String,
    val description: String,
    val nextExecution: String?
)