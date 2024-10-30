package gov.cdc.ocio.processingstatusnotifications.rulesEngine

import gov.cdc.ocio.processingstatusnotifications.SubscriptionType
import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingstatusnotifications.model.cache.NotificationSubscription
import mu.KotlinLogging

class WebsocketNotificationRule(): Rule {
    private val logger = KotlinLogging.logger {}
    /**
     * Method to evaluate existing subscription for matching rule for Websocket notifications
     * @param ruleId String
     * @param cacheService InMemoryCacheService
     * @return String
     */
    override fun evaluateAndDispatch(ruleId: String, cacheService: InMemoryCacheService): String {
        val subscribers: List<NotificationSubscription> = cacheService.getSubscription(ruleId)
        for(subscriber in subscribers) {
            if (subscriber.subscriberType == SubscriptionType.WEBSOCKET) {
                return dispatchEvent(subscriber)
            }
        }
        return ""
    }

    /**
     * Method to dispatch websocket event to the subscriber using the information saved in Datastore
     * @param subscription NotificationSubscription
     * @return String
     */
    override fun dispatchEvent(subscription: NotificationSubscription): String {
        logger.info("Websocket event dispatched for ${subscription.subscriberAddressOrUrl}")
        return "Websocket Event dispatched for ${subscription.subscriberAddressOrUrl}"
    }
}