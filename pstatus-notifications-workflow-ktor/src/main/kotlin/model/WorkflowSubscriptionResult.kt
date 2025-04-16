package gov.cdc.ocio.processingnotifications.model


/**
 * The resultant class for subscription of email/webhooks.
 *
 * @param subscriptionId String
 * @param message String
 * @param emailAddresses List<String>
 */
data class WorkflowSubscriptionResult(
    var subscriptionId: String? = null,
    var message: String? = "",
    var emailAddresses: List<String>?,
    var webhookUrl: String?
)