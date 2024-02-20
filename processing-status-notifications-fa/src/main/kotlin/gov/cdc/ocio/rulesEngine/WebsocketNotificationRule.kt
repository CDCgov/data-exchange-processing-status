package gov.cdc.ocio.rulesEngine

import gov.cdc.ocio.cache.InMemoryCacheService
import gov.cdc.ocio.model.cache.NotificationSubscription
import gov.cdc.ocio.model.http.SubscriptionType

class WebsocketNotificationRule(): Rule {
    override fun evaluate(ruleId: String, cacheService: InMemoryCacheService): Boolean {
        val subscription: NotificationSubscription = cacheService.getSubscription(ruleId) ?: return false;
        if (subscription.subscriberType == SubscriptionType.WEBSOCKET) {
            println("Websocket Rule Matched")
            dispatchEvent(subscription)
        }
        return true;
    }

    override fun dispatchEvent(subscription: NotificationSubscription) {
        println("Websocket Event dispatched")
    }
}