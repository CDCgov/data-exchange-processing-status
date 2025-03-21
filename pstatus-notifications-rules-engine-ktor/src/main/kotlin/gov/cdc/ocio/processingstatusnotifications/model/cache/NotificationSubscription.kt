package gov.cdc.ocio.processingstatusnotifications.model.cache

import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionType


/**
 * Notification subscription definition.
 *
 * @property subscriptionId String
 * @property subscriberAddressOrUrl String
 * @property subscriptionType SubscriptionType
 * @constructor
 */
class NotificationSubscription(
    val subscriptionId: String,
    val subscriberAddressOrUrl: String,
    val subscriptionType: SubscriptionType
)