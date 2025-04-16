package gov.cdc.ocio.processingnotifications.dispatch

import gov.cdc.ocio.processingnotifications.model.Subscription

abstract class Dispatcher {
    companion object {
        fun fromSubscription(subscription: Subscription): Dispatcher {
            if (!subscription.emailAddresses.isNullOrEmpty()) {
                return EmailDispatcher(subscription.emailAddresses)
            }
            if (subscription.webhookUrl != null) {
                return WebhookDispatcher(subscription.webhookUrl)
            }

            return LogDispatcher()
        }
    }
    abstract fun dispatch(data: Any)
}