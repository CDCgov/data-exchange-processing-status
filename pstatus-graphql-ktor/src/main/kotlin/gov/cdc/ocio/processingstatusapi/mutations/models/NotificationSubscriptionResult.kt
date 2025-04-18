package gov.cdc.ocio.processingstatusapi.mutations.models

import kotlinx.serialization.Serializable


/**
 * NotificationSubscriptionResult is the response class which is serialized back and forth which is in turn used for
 * getting the response which contains the subscriberId, message, and the status of subscribe/unsubscribe operations.
 *
 * @property subscriptionId String?
 * @property message String?
 * @property emailAddresses List<String>
 * @constructor
 */
@Serializable
data class NotificationSubscriptionResult(
    var subscriptionId: String? = null,
    var message: String? = "",
    var emailAddresses: List<String>?,
    var webhookUrl: String?
)
