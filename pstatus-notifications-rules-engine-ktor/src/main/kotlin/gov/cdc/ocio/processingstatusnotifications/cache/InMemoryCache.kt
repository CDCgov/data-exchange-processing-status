package gov.cdc.ocio.processingstatusnotifications.cache

import gov.cdc.ocio.processingstatusnotifications.exception.*
import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionType
import gov.cdc.ocio.processingstatusnotifications.model.cache.*
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap

/**
 * This class represents InMemoryCache to maintain state of the data at any given point for
 * subscription of rules and subscriber for the rules
 */
object InMemoryCache {
    private val logger = KotlinLogging.logger {}
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
    private val subscriberCache = HashMap<String, MutableList<NotificationSubscription>>()


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
    fun updateCacheForSubscription(subscriptionRule: String,
                                   subscriptionType: SubscriptionType,
                                   emailOrUrl: String): String {
        if (subscriptionType == SubscriptionType.EMAIL || subscriptionType == SubscriptionType.WEBSOCKET) {
            // If subscription type is EMAIL or WEBSOCKET then proceed else throw BadState Exception
            val subscriptionId = updateSubscriptionRuleCache(subscriptionRule)
            updateSubscriberCache(subscriptionId, NotificationSubscription(subscriptionId, emailOrUrl, subscriptionType))
            return subscriptionId
        } else {
            throw BadStateException("Not a valid SubscriptionType")
        }
    }

    /**
     * This method adds a new entry Map<K,V>[SubscriberRule, SubscriptionId]
     * for new rule (if it doesn't exist) in SubscriptionCache
     *
     * @param subscriptionRule String
     * @return String
     */
    private fun updateSubscriptionRuleCache(subscriptionRule: String): String {
        // Just using write lock for the entire process to ensure atomicity
        readWriteLock.writeLock().lock()
        try {
            // Try to read from the cache
            val existingSubscriptionId = subscriptionRuleCache[subscriptionRule]

            // If a subscription already exists, return it
            if (existingSubscriptionId != null) {
                logger.debug("Subscription Rule exists")
                return existingSubscriptionId
            }

            // Create a unique subscription and add it to the cache
            val subscriptionId = generateUniqueSubscriptionId()
            logger.debug("Subscription Id for this new rule has been generated $subscriptionId")
            subscriptionRuleCache[subscriptionRule] = subscriptionId
            return subscriptionId
        } finally {
            // Always release the lock
            readWriteLock.writeLock().unlock()
        }
    }


    /**
     * This method generates new unique subscriptionId for caches
     * @return String
     */
    internal fun generateUniqueSubscriptionId(): String {
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
     * @param notificationSubscriber NotificationSubscriber
     */
    private fun updateSubscriberCache(subscriptionId: String,
                                      notificationSubscriber: NotificationSubscription) {
        logger.debug("Subscriber added in subscriber cache")
        readWriteLock.writeLock().lock()
        try {
            subscriberCache.putIfAbsent(subscriptionId, mutableListOf())
            subscriberCache[subscriptionId]?.add(notificationSubscriber)
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
            logger.info("Subscription $subscriptionId doesn't exist ")
            throw BadStateException("Subscription doesn't exist")
        }
    }

    fun getSubscriptionId(ruleId: String) : String? {
        if (subscriptionRuleCache.containsKey(ruleId)) {
            return subscriptionRuleCache[ruleId]
        }
        return null
    }

    fun getSubscriptionDetails(subscriptionId: String) : List<NotificationSubscription>? {
        if (subscriberCache.containsKey(subscriptionId)) {
            return subscriberCache[subscriptionId]
        }
        return emptyList()
    }



}