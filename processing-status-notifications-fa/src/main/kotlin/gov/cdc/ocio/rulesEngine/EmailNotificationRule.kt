package gov.cdc.ocio.rulesEngine

import gov.cdc.ocio.cache.InMemoryCacheService
import gov.cdc.ocio.model.cache.NotificationSubscription
import gov.cdc.ocio.model.http.SubscriptionType

class EmailNotificationRule(): Rule {

    override fun evaluate(ruleId: String, cacheService: InMemoryCacheService): Boolean {
        val subscription: NotificationSubscription = cacheService.getSubscription(ruleId) ?: return false;
        if (subscription.subscriberType == SubscriptionType.EMAIL) {
            println("EMail Rule Matched")
            dispatchEvent(subscription)
        }
        return true;
    }

    override fun dispatchEvent(subscription: NotificationSubscription) {
        TODO("Not yet implemented")
    }
}