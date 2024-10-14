package gov.cdc.ocio.rulesengine.models

/**
 * The class which defines the RuleAction attributes
 * @param name String
 * @param parameters Map<String,Any>
 */
data class RuleAction(
    val name: String, // Action name or type (e.g., "SEND_EMAIL_NOTIFICATION", "SEND_WEB_HOOK_NOTIFICATION")
    val parameters: Map<String, Any> // Parameters required to perform the action
)
