package gov.cdc.ocio.processingnotifications.model


data class WorkflowStatus(
    val workflowId: String,
    val status: String,
    val cronSchedule: String
)