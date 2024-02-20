package gov.cdc.ocio.cache

import gov.cdc.ocio.exceptions.BadStateException
import gov.cdc.ocio.model.cache.NotificationSubscription
import gov.cdc.ocio.model.http.SubscriptionType
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
        // Try to read from existing cache to see an existing subscription rule
        var existingSubscriptionId: String? = null
        readWriteLock.readLock().lock()
        try {
            existingSubscriptionId = subscriptionRuleCache.get(subscriptionRule)
        } finally {
            readWriteLock.readLock().unlock()
        }

        // if subscription doesn't exist, it will add it else it will return the existing subscription id
        return if (existingSubscriptionId != null) {
            logger.info("Subscription Rule exists")
            existingSubscriptionId
        } else {
            // create unique subscription
            val subscriptionId = generateUniqueSubscriptionId()
            logger.info("Subscription Id for this new rule has been generated $subscriptionId")
            readWriteLock.writeLock().lock()
            try {
                subscriptionRuleCache.put(subscriptionRule, subscriptionId)
            } finally {
                readWriteLock.writeLock().unlock()
            }
            subscriptionId
        }
    }

    /**
     * This method generates new unique susbscriptionId for caches
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
        logger.info("Subscriber added in subscriber cache")
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
            val subscribers = subscriberCache[subscriptionId]?.filter { it.subscriptionId != subscriptionId }.orEmpty().toMutableList()

            readWriteLock.writeLock().lock()
            try {
                subscriberCache.replace(subscriptionId, subscribers)
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