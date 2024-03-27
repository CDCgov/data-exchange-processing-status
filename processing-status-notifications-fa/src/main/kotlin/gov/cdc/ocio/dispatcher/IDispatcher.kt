package gov.cdc.ocio.dispatcher

import gov.cdc.ocio.model.cache.NotificationSubscription

interface IDispatcher {

    fun dispatchEvent(subscription: NotificationSubscription): String
}