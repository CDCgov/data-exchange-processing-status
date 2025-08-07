package gov.cdc.ocio.processingnotifications.model

import gov.cdc.ocio.processingnotifications.temporal.WorkflowFailureInfo


/**
 * Represents the status of a workflow, providing details about its context, execution, and potential errors.
 *
 * @property workflowId String
 * @property taskName String
 * @property taskQueue String
 * @property description String
 * @property workerAttached Boolean?
 * @property status String
 * @property schedule CronSchedule
 * @property workflowImplClassName String?
 * @property workflowFailureInfo WorkflowFailureInfo
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
    val workflowImplClassName: String?,
    val workflowFailureInfo: WorkflowFailureInfo?
)
