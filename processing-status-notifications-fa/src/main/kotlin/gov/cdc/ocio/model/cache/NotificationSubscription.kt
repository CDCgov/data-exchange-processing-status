package gov.cdc.ocio.model.cache

import gov.cdc.ocio.model.http.SubscriptionType

class NotificationSubscription(val subscriberAddressOrUrl: String,
                               val subscriberType: SubscriptionType
)