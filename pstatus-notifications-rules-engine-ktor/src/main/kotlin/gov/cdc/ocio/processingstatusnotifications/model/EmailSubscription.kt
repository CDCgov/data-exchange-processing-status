package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.processingstatusnotifications.model.message.Status

/**
 * Email Subscription data class which is serialized back and forth
 */
data class EmailSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val email: String,
    val service: String,
    val action: String,
    val status: Status
)