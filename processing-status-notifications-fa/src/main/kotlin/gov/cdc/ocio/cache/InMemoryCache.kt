package gov.cdc.ocio.cache

import gov.cdc.ocio.exceptions.BadStateException
import gov.cdc.ocio.model.cache.NotificationSubscriber
import gov.cdc.ocio.model.message.SubscriptionType
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap


object InMemoryCache {
    private val logger = KotlinLogging.logger {};
    private val readWriteLock = ReentrantReadWriteLock()

    // Cache to store "SubscriptionRule HashSet ->  SubscriptionId"
    private val subscriptionRuleCache = HashMap<String, String>();
    // Cache to store "SubscriptionId ->  Subscriber Info (Email or Url and type of subscription)"
    private val subscriberCache = HashMap<String, NotificationSubscriber>();


    fun updateCacheForSubscription(subscriptionRule: String,
                    subscriptionType: SubscriptionType,
                    emailOrUrl: String): String {
        if (subscriptionType == SubscriptionType.EMAIL || subscriptionType == SubscriptionType.WEBSOCKET) {
            // If subscription type is EMAIL or WEBSOCKET then proceed else throw BAdState Exception
            val subscriptionId = updateSubscriptionRuleCache(subscriptionRule)
            updateSubscriberCache(subscriptionId, NotificationSubscriber(emailOrUrl, subscriptionType))
            return subscriptionId
        } else {
            throw BadStateException("Not a valid SubscriptionType")
        }
    }

    fun unsubscribeSubscriber(subscriptionId: String): Boolean {
      if (subscriberCache.containsKey(subscriptionId)) {
          val subscriber = subscriberCache.get(subscriptionId);
          readWriteLock.writeLock().lock()
          try {
              subscriberCache.remove(subscriptionId)
          } finally {
              readWriteLock.writeLock().unlock()
          }
          logger.info("Subscriber ${subscriber?.subscriberAddressOrUrl} " +
                  "has been removed successfully for subscription type of " +
                  "${subscriber?.subscriberType} ")
          return true
      } else {
          logger.info("Subscription $subscriptionId doesn't exist ")
          return false;
      }
    }

    private fun updateSubscriptionRuleCache(subscriptionRule: String): String {
        // Try to read from existing cache to see an existing subscription rule
        var existingSubscriptionId: String? = null;
        readWriteLock.readLock().lock()
        try {
            existingSubscriptionId = subscriptionRuleCache.get(subscriptionRule)
        } finally {
            readWriteLock.readLock().unlock()
        }

        // if subscription doesn't exist, it will add it else it will return the existing subscription id
        if (existingSubscriptionId != null) {
            logger.info("Subscription Rule exists")
            return existingSubscriptionId
        } else {
            // create unique subscription
            val subscriptionId = generateUniqueSubscriptionId();
            logger.info("Subscription Id for this new rule has been generated $subscriptionId")
            readWriteLock.writeLock().lock()
            try {
                subscriptionRuleCache.put(subscriptionRule, subscriptionId)
            } finally {
                readWriteLock.writeLock().unlock()
            }
            return subscriptionId
        }
    }

    // method to generate unique subscription id and add it to the pool.
    private fun generateUniqueSubscriptionId(): String {
        // TODO: This could be handled to background task to populate
        //  uniqueSubscriptionIds bucket of size 20 or 50, may be?
        var subscriptionId = UUID.randomUUID().toString()
        while(subscriberCache.contains(subscriptionId)) {
            subscriptionId = UUID.randomUUID().toString()
        }
        return subscriptionId
    }

    private fun updateSubscriberCache(subscriptionId: String,
                                      notificationSubscriber: NotificationSubscriber) {
        logger.info("Subscriber added in subscriber cache")
        readWriteLock.writeLock().lock()
        try {
            subscriberCache.put(subscriptionId, notificationSubscriber)
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }


}