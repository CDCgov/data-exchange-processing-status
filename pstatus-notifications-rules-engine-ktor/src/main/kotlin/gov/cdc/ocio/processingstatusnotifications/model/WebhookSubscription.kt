package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.processingstatusnotifications.model.message.Status

/**
 * Webhook Subscription data class which is serialized back and forth
 */
data class WebhookSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val url: String,
    val service: String,
    val action: String,
    val status: Status
)