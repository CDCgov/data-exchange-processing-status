package gov.cdc.ocio.processingstatusnotifications.model.cache

import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionType

class NotificationSubscription(val subscriptionId: String,
                               val subscriberAddressOrUrl: String,
                               val subscriberType: SubscriptionType
)