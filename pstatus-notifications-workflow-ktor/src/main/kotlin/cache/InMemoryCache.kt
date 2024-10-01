package gov.cdc.ocio.processingnotifications.cache

import gov.cdc.ocio.processingnotifications.model.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap

/**
 * This class represents InMemoryCache to maintain state of the data at any given point for
 * subscription of rules and subscriber for the rules
 */
object InMemoryCache {
    private val readWriteLock = ReentrantReadWriteLock()
    /*
    Cache to store "SubscriptionId ->  Subscriber Info (Email or Url and type of subscription)"
    subscriberCache = HashMap<String, NotificationSubscriber>()
    */
    private val subscriberCache = HashMap<String, MutableList<NotificationSubscriptionResponse>>()


    /**
     * If Success, this method updates Two Caches for New Subscription:
     *        a. First Cache with subscription Rule and respective subscriptionId,
     *           if it doesn't exist,or it returns existing subscription id.
     *        b. Second cache is subscriber cache where the subscription id is mapped to emailId of subscriber
     *           or websocket url with the type of subscription
     *

     * @return String
     */
    fun updateCacheForSubscription(workflowId:String, baseSubscription: BaseSubscription): WorkflowSubscriptionResult {
       // val uuid = generateUniqueSubscriptionId()
        try {

          updateSubscriberCache(workflowId,
              NotificationSubscriptionResponse(subscriptionId = workflowId, subscription = baseSubscription))
          return WorkflowSubscriptionResult(subscriptionId = workflowId, message = "Successfully subscribed for $workflowId", deliveryReference = baseSubscription.deliveryReference)
      }
      catch (e: Exception){
          return WorkflowSubscriptionResult(subscriptionId = workflowId, message = e.message, deliveryReference = baseSubscription.deliveryReference)
      }

    }

    fun updateCacheForUnSubscription(workflowId:String): WorkflowSubscriptionResult {
       try {

            unsubscribeSubscriberCache(workflowId)
            return WorkflowSubscriptionResult(subscriptionId = workflowId, message = "Successfully unsubscribed Id = $workflowId", deliveryReference = "")
        }
        catch (e: Exception){
            return WorkflowSubscriptionResult(subscriptionId = workflowId, message = e.message,"")
        }

    }

    /**
     * This method adds to the subscriber cache the new entry of subscriptionId to the NotificationSubscriber
     *
     * @param subscriptionId String

     */
    private fun updateSubscriberCache(subscriptionId: String,
                                      notificationSubscriptionResponse: NotificationSubscriptionResponse) {
        //logger.debug("Subscriber added in subscriber cache")
        readWriteLock.writeLock().lock()
        try {
            subscriberCache.putIfAbsent(subscriptionId, mutableListOf())
            subscriberCache[subscriptionId]?.add(notificationSubscriptionResponse)
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    /**
     * This method unsubscribes the subscriber from the subscriber cache
     * by removing the Map<K,V>[subscriptionId, NotificationSubscriber]
     * entry from cache but keeps the susbscriptionRule in subscription
     * cache for any other existing subscriber needs.
     *
     * @param subscriptionId String
     * @return Boolean
     */
    private fun unsubscribeSubscriberCache(subscriptionId: String): Boolean {
        if (subscriberCache.containsKey(subscriptionId)) {
            val subscribers = subscriberCache[subscriptionId]?.filter { it.subscriptionId == subscriptionId }.orEmpty().toMutableList()

            readWriteLock.writeLock().lock()
            try {
                subscriberCache.remove(subscriptionId, subscribers)
            } finally {
                readWriteLock.writeLock().unlock()
            }
            return true
        } else {
             throw Exception("Subscription doesn't exist")
        }
    }
}