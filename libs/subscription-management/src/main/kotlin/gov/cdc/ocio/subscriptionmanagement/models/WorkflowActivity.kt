package gov.cdc.ocio.subscriptionmanagement.models

/**
 * The class which defines the workflow activity attributes which gets subscribed and persisted
 * @param name String
 * @param parameters Map<String,Any>
 */
data class WorkflowActivity(
    val name: String, // Action name or type (e.g., "SEND_EMAIL_NOTIFICATION", "SEND_WEB_HOOK_NOTIFICATION")
    val parameters: Map<String, Any> // Parameters required to perform the action
)
