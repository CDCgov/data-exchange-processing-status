package gov.cdc.ocio.processingstatusnotifications.model


/**
 * Webhook Subscription data class which is serialized back and forth
 */
data class WebhookSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String,
    val mvelCondition: String,
    val webhookUrl: String
)