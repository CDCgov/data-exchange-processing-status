package gov.cdc.ocio.processingnotifications.model

import gov.cdc.ocio.types.model.WorkflowSubscription

data class WebhookContent(
    val subscriptionId: String,
    val workflowType: WorkflowType,
    val subscription: WorkflowSubscription,
    val triggered: String,
    val content: Any
)