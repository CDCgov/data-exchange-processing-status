package gov.cdc.ocio.processingnotifications.model

import gov.cdc.ocio.types.model.WorkflowSubscription

data class TemporalSubscription<T: WorkflowSubscription>(
    val workflowSubscription: T,
    val taskQueue: WorkflowTaskQueue,
    val description: String,
)