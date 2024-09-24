package gov.cdc.ocio.rulesenginelib


import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

// Enum to define the rule type
@Serializable
enum class RuleType{
    NONE,
    EASY_RULES,
    WORKFLOW
}

// Serializable data class for email notifications
@Serializable
 class EmailNotification(
    @JsonProperty("emails")
    val emails: List<String>
)

// Serializable data class for webhook notifications
@Serializable
class WebhookNotification(
    @JsonProperty("url")
    val url: String
)

// Data class representing a Rule
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
 class Rule(
    @JsonProperty("id")
    var id : String? =null,
    @JsonProperty("ruleId")
    var ruleId: String? = null,
    @JsonProperty("workflowId")
    var workflowId: String? = null,
    @JsonProperty("type")
    var type: RuleType=RuleType.NONE,
    @JsonProperty("condition")
    var condition: String?=null,
    @JsonProperty("emailNotification")
    var emailNotification: EmailNotification? = null,
    @JsonProperty("webhookNotification")
    var webhookNotification: WebhookNotification? = null


)
