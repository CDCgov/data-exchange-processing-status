package gov.cdc.ocio.processingnotifications.model

/**
 * The resultant class for subscription of email/webhooks
 * @param subscriptionId String
 * @param message String
 * @param deliveryReference String
 */
data class WorkflowSubscriptionResult(
    var subscriptionId: String? = null,
    var message: String? = "",
    var deliveryReference:String
)