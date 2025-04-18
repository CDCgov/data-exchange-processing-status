package gov.cdc.ocio.types.model

import kotlinx.serialization.Serializable


/**
 * The resultant class for subscription of email/webhooks.
 *
 * @param subscriptionId String
 * @param message String
 * @param emailAddresses List<String>
 */
@Serializable
data class WorkflowSubscriptionResult(
    var subscriptionId: String? = null,
    var message: String? = "",
    var emailAddresses: List<String>?,
    var webhookUrl: String?
)