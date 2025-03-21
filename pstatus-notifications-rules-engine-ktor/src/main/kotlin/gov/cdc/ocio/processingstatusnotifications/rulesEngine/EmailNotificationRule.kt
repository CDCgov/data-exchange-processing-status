package gov.cdc.ocio.processingstatusnotifications.rulesEngine

import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionType
import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingstatusnotifications.dispatcher.EmailDispatcher
import gov.cdc.ocio.processingstatusnotifications.model.cache.NotificationSubscription
import mu.KotlinLogging

/**
 * Class to evaluate existing rules in Datastore for email notifications.
 * If matching rule exist, we can send an email using EmailDispatcher
 */
class EmailNotificationRule(): Rule {
    private val logger = KotlinLogging.logger {}

    /**
     * Method to evaluate existing subscription for matching rule for Email notifications
     * @param ruleId String
     * @param cacheService InMemoryCacheService
     * @return String
     */
    override fun evaluateAndDispatch(ruleId: String, cacheService: InMemoryCacheService): String {
        val subscribers: List<NotificationSubscription> = cacheService.getSubscription(ruleId)
        for(subscriber in subscribers) {
            if (subscriber.subscriptionType == SubscriptionType.EMAIL) {
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
        val emailDispatcher = EmailDispatcher()
        return emailDispatcher.dispatchEvent(subscription)
    }
}