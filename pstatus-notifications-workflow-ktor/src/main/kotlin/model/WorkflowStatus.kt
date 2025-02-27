package gov.cdc.ocio.processingnotifications.model


/**
 * Model for the workflow status.
 *
 * @property workflowId String
 * @property status String
 * @property cronSchedule String
 * @property cronScheduleDescription String
 * @constructor
 */
data class WorkflowStatus(
    val workflowId: String,
    val status: String,
    val cronSchedule: String,
    val cronScheduleDescription: String
)