package gov.cdc.ocio.model.cache

import gov.cdc.ocio.model.http.SubscriptionType

class NotificationSubscriber(val subscriberAddressOrUrl: String,
                             val subscriberType: SubscriptionType
)