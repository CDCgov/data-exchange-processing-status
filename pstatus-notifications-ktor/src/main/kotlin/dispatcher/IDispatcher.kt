package gov.cdc.ocio.processingstatusnotifications.dispatcher

import gov.cdc.ocio.processingstatusnotifications.model.cache.*

interface IDispatcher {

    fun dispatchEvent(subscription: NotificationSubscription): String
}