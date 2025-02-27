package gov.cdc.ocio.processingnotifications.model


/**
 * Model for the workflow status.
 *
 * @property workflowId String
 * @property status String
 * @property cronSchedule CronSchedule
 * @constructor
 */
data class WorkflowStatus(
    val workflowId: String,
    val status: String,
    val schedule: CronSchedule
)

/**
 * Raw cron schedule and its human-readable form.
 *
 * @property raw String
 * @property description String
 * @constructor
 */
data class CronSchedule(
    val cron: String,
    val description: String
)