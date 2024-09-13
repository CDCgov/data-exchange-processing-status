package gov.cdc.ocio.processingnotifications.cache


import gov.cdc.ocio.processingnotifications.model.BaseSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult

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
    fun updateSubscriptionPreferences(workflowId:String, baseSubscription: BaseSubscription): WorkflowSubscriptionResult {
        try {
             return  InMemoryCache.updateCacheForSubscription(workflowId,baseSubscription)
        } catch (e: Exception) {
            throw e
        }
    }

    fun updateDeadlineCheckUnSubscriptionPreferences(workflowId:String): WorkflowSubscriptionResult {
        try {
            return  InMemoryCache.updateCacheForUnSubscription(workflowId)
        } catch (e: Exception) {
            throw e
        }
    }

}
