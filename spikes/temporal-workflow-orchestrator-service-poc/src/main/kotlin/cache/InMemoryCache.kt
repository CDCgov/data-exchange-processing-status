package gov.cdc.ocio.processingnotifications.cache

import gov.cdc.ocio.processingnotifications.*
import gov.cdc.ocio.processingnotifications.model.DeadLineCheckNotificationSubscription
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap

/**
 * This class represents InMemoryCache to maintain state of the data at any given point for
 * subscription of rules and subscriber for the rules
 */
object InMemoryCache {
    private val readWriteLock = ReentrantReadWriteLock()

    /*
    Cache to store "SubscriptionRule HashSet ->  SubscriptionId"
    subscriptionRuleCache = HashMap<String, String>()
     */
    private val subscriptionRuleCache = HashMap<String, String>()

    /*
    Cache to store "SubscriptionId ->  Subscriber Info (Email or Url and type of subscription)"
    subscriberCache = HashMap<String, NotificationSubscriber>()
    */
    private val subscriberCache = HashMap<String, MutableList<DeadLineCheckNotificationSubscription>>()


    /**
     * If Success, this method updates Two Caches for New Subscription:
     *        a. First Cache with subscription Rule and respective subscriptionId,
     *           if it doesn't exist,or it returns existing subscription id.
     *        b. Second cache is subscriber cache where the subscription id is mapped to emailId of subscriber
     *           or websocket url with the type of subscription
     *
     * @param subscriptionRule String - Hashcode of object with all the required fields
     * @param subscriptionType SubscriptionType - Currently supports only 'Email' or 'Websocket'
     * @param emailOrUrl String - Valid EmailId or Valid WebSocket Url
     * @return String
     */
    fun updateDeadLineCheckCacheForSubscription(deadlineCheckSubscription: DeadlineCheckSubscription): DeadlineCheckSubscriptionResult {
        val uuid = generateUniqueSubscriptionId()
        try {

          updateDeadLineCheckSubscriberCache(uuid,
              DeadLineCheckNotificationSubscription(subscriptionId = uuid,deadlineCheckSubscription= deadlineCheckSubscription))
          return DeadlineCheckSubscriptionResult(subscriptionId = uuid, message = "Successfully subscribed for $uuid", deliveryReference = deadlineCheckSubscription.deliveryReference)
      }
      catch (e: Exception){
          return DeadlineCheckSubscriptionResult(subscriptionId = uuid, message = e.message, deliveryReference = deadlineCheckSubscription.deliveryReference)
      }

    }


    /**
     * This method generates new unique susbscriptionId for caches
     * @return String
     */
    private fun generateUniqueSubscriptionId(): String {
        // TODO: This could be handled to background task to populate
        //  uniqueSubscriptionIds bucket of size 20 or 50, may be?
        var subscriptionId = UUID.randomUUID().toString()
        while(subscriberCache.contains(subscriptionId)) {
            subscriptionId = UUID.randomUUID().toString()
        }
        return subscriptionId
    }

    /**
     * This method adds to the subscriber cache the new entry of subscriptionId to the NotificationSubscriber
     *
     * @param subscriptionId String
     * @param deadLineCheckNotificationSubscription DeadLineCheckNotificationSubscription
     */
    private fun updateDeadLineCheckSubscriberCache(subscriptionId: String,
                                                   deadLineCheckNotificationSubscription: DeadLineCheckNotificationSubscription) {
        //logger.debug("Subscriber added in subscriber cache")
        readWriteLock.writeLock().lock()
        try {
            subscriberCache.putIfAbsent(subscriptionId, mutableListOf())
            subscriberCache[subscriptionId]?.add(deadLineCheckNotificationSubscription)
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
    fun unsubscribeSubscriber(subscriptionId: String): Boolean {
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

    fun getSubscriptionId(ruleId: String) : String? {
        if (subscriptionRuleCache.containsKey(ruleId)) {
            return subscriptionRuleCache[ruleId]
        }
        return null
    }

    fun getSubscriptionDetails(subscriptionId: String) : List<DeadLineCheckNotificationSubscription>? {
        if (subscriberCache.containsKey(subscriptionId)) {
            return subscriberCache[subscriptionId]
        }
        return emptyList()
    }

}