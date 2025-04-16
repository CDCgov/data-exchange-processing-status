package gov.cdc.ocio.processingnotifications.dispatch

import gov.cdc.ocio.processingnotifications.model.Subscription
import gov.cdc.ocio.types.model.Notifiable
import mu.KotlinLogging

abstract class Dispatcher {
    protected val logger = KotlinLogging.logger {}

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
    open fun dispatch(payload: Notifiable) {
        logger.info("dispatched notification $payload")
    }
}