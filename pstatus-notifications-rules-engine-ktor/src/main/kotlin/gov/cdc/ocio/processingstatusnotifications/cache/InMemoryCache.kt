package gov.cdc.ocio.processingstatusnotifications.cache

import gov.cdc.ocio.processingstatusnotifications.exception.*
import gov.cdc.ocio.processingstatusnotifications.model.Subscription
import mu.KotlinLogging
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap


/**
 * This class represents InMemoryCache to maintain state of the data at any given point for
 * subscription of rules and subscriber for the rules
 */
object InMemoryCache {

    private val logger = KotlinLogging.logger {}

    private val readWriteLock = ReentrantReadWriteLock()

    private val subscriptionCache = HashMap<String, Subscription>()

    /**
     * Updates the cache for the subscription.
     *
     * @param subscriptionId String
     * @param subscription Subscription
     */
    fun subscribe(
        subscriptionId: String,
        subscription: Subscription
    ) {
        readWriteLock.writeLock().lock()
        try {
            subscriptionCache.putIfAbsent(subscriptionId, subscription)
            logger.debug("Subscriber added in subscriber cache")

        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    /**
     * Unsubscribes from the provided subscription id.
     * @param subscriptionId String
     * @throws BadStateException thrown if subscription if not found.
     */
    @Throws(BadStateException::class)
    fun unsubscribe(subscriptionId: String) {
        if (subscriptionCache.containsKey(subscriptionId)) {
            readWriteLock.writeLock().lock()
            try {
                subscriptionCache.remove(subscriptionId)
            } finally {
                readWriteLock.writeLock().unlock()
            }
        } else {
            logger.info("Subscription $subscriptionId doesn't exist")
            throw BadStateException("Subscription doesn't exist")
        }
    }

    fun findSubscriptionId(subscription: Subscription)
        = subscriptionCache.entries.firstOrNull { subscription == it.value }?.key

    fun getSubscriptionDetails(subscriptionId: String) = subscriptionCache[subscriptionId]
}