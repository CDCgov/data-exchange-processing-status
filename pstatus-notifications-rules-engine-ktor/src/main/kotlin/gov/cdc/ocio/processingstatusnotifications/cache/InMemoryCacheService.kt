package gov.cdc.ocio.processingstatusnotifications.cache

import gov.cdc.ocio.processingstatusnotifications.exception.*
import gov.cdc.ocio.processingstatusnotifications.model.EmailNotification
import gov.cdc.ocio.processingstatusnotifications.model.Subscription
import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionType
import gov.cdc.ocio.processingstatusnotifications.model.cache.*
import gov.cdc.ocio.processingstatusnotifications.model.message.Status
import java.util.*


/**
 * This class is a service that interacts with InMemory Cache in order to subscribe/unsubscribe users.
 */
class InMemoryCacheService {

    /**
     * This method creates a hash of the rule keys (dataStreamId, stageName, dataStreamRoute, statusType)
     * to use as a key for SubscriptionRuleCache and creates a new or existing subscription (if exist)
     * and creates a new entry in subscriberCache for the user with the subscriptionRuleKey.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param service String
     * @param action String
     * @param status Status
     * @param emailOrUrl String
     * @param subscriptionType SubscriptionType
     * @return String?
     */
    fun updateNotificationsPreferences(
        dataStreamId: String,
        dataStreamRoute: String,
        service: String,
        action: String,
        status: Status,
        emailOrUrl: String,
        subscriptionType: SubscriptionType
    ): String {
        try {
            val subscriptionRule = SubscriptionRule(
                dataStreamId,
                dataStreamRoute,
                null,
                ""
            )
            val subscription = Subscription(
                subscriptionId = UUID.randomUUID().toString(),
                subscriptionRule,
                EmailNotification(listOf(emailOrUrl))
            )
            val subscriptionId =
                InMemoryCache.updateCacheForSubscription(subscription, subscriptionType, emailOrUrl)
            return subscriptionId
        } catch (e: BadStateException) {
            throw e
        }
    }

    /**
     * This method removes subscriber from subscription rule.
     * If the rule doesn't exist then it throws BadStateException
     *
     * @param subscriptionId String
     * @return Boolean
     */
    fun unsubscribeNotifications(subscriptionId: String): Boolean {
        try {
            return InMemoryCache.unsubscribeSubscriber(subscriptionId)
        } catch (e: BadStateException) {
            throw e
        }
    }

    /**
     * Checks for subscription rule and gets the subscriptionId, using the subscription id to retrieve the
     * NotificationSubscription details.
     *
     * @param ruleId String
     * @return Boolean
     */
    fun getSubscription(ruleId: String): List<NotificationSubscription> {
        try {
            val subscriptionId = InMemoryCache.getSubscriptionId(ruleId)
            if (subscriptionId != null) {
                return InMemoryCache.getSubscriptionDetails(subscriptionId).orEmpty()
            }
            return emptyList()
        } catch (e: BadStateException) {
            throw e
        }
    }
}
