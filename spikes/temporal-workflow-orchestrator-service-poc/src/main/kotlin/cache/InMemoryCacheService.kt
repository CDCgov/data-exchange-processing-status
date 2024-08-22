package gov.cdc.ocio.processingnotifications.cache

import gov.cdc.ocio.processingnotifications.DeadlineCheckSubscription
import gov.cdc.ocio.processingnotifications.DeadlineCheckSubscriptionResult
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
    fun updateDeadlineCheckSubscriptionPreferences(workflowId:String, deadLineCheckSubscription: DeadlineCheckSubscription): DeadlineCheckSubscriptionResult {
        try {
             return  InMemoryCache.updateDeadLineCheckCacheForSubscription(workflowId,deadLineCheckSubscription)
        } catch (e: Exception) {
            throw e
        }
    }

    fun updateDeadlineCheckUnSubscriptionPreferences(workflowId:String): DeadlineCheckSubscriptionResult {
        try {
            return  InMemoryCache.updateDeadLineCheckCacheForUnSubscription(workflowId)
        } catch (e: Exception) {
            throw e
        }
    }

}
