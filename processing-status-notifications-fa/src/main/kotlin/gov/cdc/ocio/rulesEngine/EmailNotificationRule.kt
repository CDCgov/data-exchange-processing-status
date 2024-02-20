package gov.cdc.ocio.rulesEngine

import gov.cdc.ocio.cache.InMemoryCacheService
import gov.cdc.ocio.model.cache.NotificationSubscription
import gov.cdc.ocio.model.http.SubscriptionType

/**
 * Class to evaluate existing rules in Datastore for email notifications.
 * If matching rule exist, we can send an email using EmailDispatcher
 */
class EmailNotificationRule(): Rule {

    /**
     * Method to evaluate existing subscription for matching rule for Email notifications
     * @param ruleId String
     * @param cacheService InMemoryCacheService
     * @return String
     */
    override fun evaluateAndDispatch(ruleId: String, cacheService: InMemoryCacheService): String {
        val subscribers: List<NotificationSubscription> = cacheService.getSubscription(ruleId)
        for(subscriber in subscribers) {
            if (subscriber.subscriberType == SubscriptionType.EMAIL) {
                return dispatchEvent(subscriber)
            }
        }
        return ""
    }

    /**
     * Method to dispatch email to the subscriber using the information saved in Datastore
     * @param subscription NotificationSubscription
     * @return String
     */
    override fun dispatchEvent(subscription: NotificationSubscription): String {
        return "Email Event dispatched for ${subscription.subscriberAddressOrUrl}"
    }
}