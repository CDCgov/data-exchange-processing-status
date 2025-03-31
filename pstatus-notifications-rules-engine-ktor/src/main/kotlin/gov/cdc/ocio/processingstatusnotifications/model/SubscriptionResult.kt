package gov.cdc.ocio.processingstatusnotifications.model

/**
 * The resultant class for subscription of email/webhooks
 */
data class SubscriptionResult(
    var subscriptionId: String? = null,
    var timestamp: Long? = null,
    var status: Boolean? = false,
    var message: String? = ""
)