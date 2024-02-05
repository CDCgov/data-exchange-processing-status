package gov.cdc.ocio.model.cache

import gov.cdc.ocio.model.message.SubscriptionType

class NotificationSubscriber(val subscriberAddressOrUrl: String,
                             val subscriberType: SubscriptionType)