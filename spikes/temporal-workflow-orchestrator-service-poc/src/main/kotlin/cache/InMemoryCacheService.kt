package gov.cdc.ocio.processingnotifications.cache

import gov.cdc.ocio.processingnotifications.DeadlineCheckSubscription
import gov.cdc.ocio.processingnotifications.DeadlineCheckSubscriptionResult
import gov.cdc.ocio.processingnotifications.model.DeadLineCheckNotificationSubscription

/**
 * This class is a service that interacts with InMemory Cache in order to subscribe/unsubscribe users
 */
class InMemoryCacheService {

    /**
     * This method creates a hash of the rule keys (dataStreamId, stageName, dataStreamRoute, statusType)
     * to use as a key for SubscriptionRuleCache and creates a new or existing subscription (if exist)
     * and creates a new entry in subscriberCache for the user with the susbscriptionRuleKey
     *

     */
    fun updateDeadlineCheckNotificationPreferences(deadLineCheckSubscription: DeadlineCheckSubscription): DeadlineCheckSubscriptionResult {
        try {
             return  InMemoryCache.updateDeadLineCheckCacheForSubscription(deadLineCheckSubscription)
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * This methods checks for subscription rule and gets the subscriptionId.
     * In turn uses the subscription Id to retrieve the NotificationSubscription details
     * @param ruleId String
     * @return Boolean
     */
    fun getSubscription(ruleId: String): List<DeadLineCheckNotificationSubscription> {
        try {
            val subscriptionId = InMemoryCache.getSubscriptionId(ruleId)
            if (subscriptionId != null) {
                return InMemoryCache.getSubscriptionDetails(subscriptionId).orEmpty()
            }
            return emptyList()
        } catch (e: Exception) {
            throw e
        }
    }
}
