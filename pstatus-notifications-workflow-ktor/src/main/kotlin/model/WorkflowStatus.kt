package gov.cdc.ocio.processingnotifications.model

import java.time.OffsetDateTime


/**
 * Model for the workflow status.
 *
 * @property workflowId String
 * @property taskName String
 * @property taskQueue String
 * @property description String
 * @property status String
 * @property workerAttached Boolean?
 * @property schedule CronSchedule
 * @property workflowImplClassName String?
 * @constructor
 */
data class WorkflowStatus(
    val workflowId: String,
    val taskName: String,
    val taskQueue: String,
    val description: String,
    val workerAttached: Boolean?,
    val status: String,
    val schedule: CronSchedule,
    val workflowImplClassName: String?
)

/**
 * Raw cron schedule and its human-readable form.
 *
 * @property cron String?
 * @property description String?
 * @property lastRun OffsetDateTime?
 * @property nextExecution String?
 * @constructor
 */
data class CronSchedule(
    val cron: String?,
    val description: String?,
    val lastRun: OffsetDateTime?,
    val nextExecution: String?
)