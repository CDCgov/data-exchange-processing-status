package gov.cdc.ocio.processingstatusapi.mutations.models

import kotlinx.serialization.Serializable


/**
 * NotificationSubscriptionResult is the response class which is serialized back and forth which is in turn used for
 * getting the response which contains the subscriberId, message, and the status of subscribe/unsubscribe operations.
 *
 * @param subscriptionId
 * @param message
 * @param deliveryReference
 */
@Serializable
data class NotificationSubscriptionResult(
    var subscriptionId: String? = null,
    var message: String? = "",
    var deliveryReference:String
)
