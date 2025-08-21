package gov.cdc.ocio.processingstatusnotifications.subscription

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import gov.cdc.ocio.processingstatusnotifications.model.Subscription
import java.util.concurrent.TimeUnit


/**
 * Cached subscription loader for the notification subscriptions.
 *
 * @property subscriptionLoaderImpl [SubscriptionLoader] - implementation that backs the cached loader; i.e. database
 * subscription loader such as the [DatabaseSubscriptionLoader]
 * @property subscriptionCache
 * @property subscriptionListCache
 * @constructor
 */
class CachedSubscriptionLoader(
    private val subscriptionLoaderImpl: SubscriptionLoader
) : SubscriptionLoader {

    companion object {
        // Evict subscription content after a reasonable period of time even though in theory, subscription content
        // should NEVER be changed without the knowledge of this service.
        private const val CACHED_SUBSCRIPTION_CONTENT_DURATION_MINUTES = 15L

        // The list of subscriptions can change often, so expire the list after a short period of time.
        private const val CACHED_SUBSCRIPTION_LIST_DURATION_MINUTES = 5L
    }

    private val subscriptionCache = CacheBuilder.newBuilder()
        .expireAfterWrite(CACHED_SUBSCRIPTION_CONTENT_DURATION_MINUTES, TimeUnit.MINUTES) // Expire entries after 15 minutes
        .build(
            object : CacheLoader<String, Subscription>() {
                override fun load(subscriptionId: String): Subscription {
                    return subscriptionLoaderImpl.getSubscription(subscriptionId)
                }
            }
        )

    // Note: The "memoized" guava is another option in lieu of cache, but it's not a great one for our needs.
    // Although memoized entries are a keyless cache, it requires you to have a static function you call to get the
    // value, which isn't ideal for our purpose.
    private val subscriptionListCache = CacheBuilder.newBuilder()
        .expireAfterWrite(CACHED_SUBSCRIPTION_LIST_DURATION_MINUTES, TimeUnit.MINUTES)
        .build(
            object : CacheLoader<Int, List<Subscription>>() {
                override fun load(unused : Int): List<Subscription> {
                    return subscriptionLoaderImpl.getSubscriptions()
                }
            }
        )

    /**
     * Defines the interface for retrieving all the notification subscriptions.
     *
     * @return [List]<[Subscription]>
     */
    override fun getSubscriptions(): List<Subscription> = subscriptionListCache.get(0)

    /**
     * Get the subscription associated with the subscription id provided.
     *
     * @param subscriptionId [String]
     * @return [Subscription]
     */
    override fun getSubscription(subscriptionId: String): Subscription = subscriptionCache.get(subscriptionId)

    /**
     * Upserts a subscription -- if it does not exist it is added, otherwise the subscription is replaced.
     *
     * @param subscriptionId String
     * @param subscription Subscription
     * @return Boolean - true if successful, false otherwise
     */
    override fun upsertSubscription(subscriptionId: String, subscription: Subscription): Boolean {
        // Invalid this subscription in the subscription cache.
        subscriptionCache.invalidate(subscriptionId)
        // Invalidate the subscription list cache to force a new list retrieval in case the file was added, not updated.
        subscriptionListCache.invalidateAll()

        return subscriptionLoaderImpl.upsertSubscription(subscriptionId, subscription)
    }

    /**
     * Removes the subscription associated with the subscription id provided.
     *
     * @param subscriptionId String
     * @return Boolean - true if successful, false otherwise
     */
    override fun removeSubscription(subscriptionId: String): Boolean {
        // Invalidate this subscription from the subscription cache.
        subscriptionCache.invalidate(subscriptionId)
        // Invalidate the subscription list cache to force a new list retrieval.
        subscriptionListCache.invalidateAll()

        return subscriptionLoaderImpl.removeSubscription(subscriptionId)
    }

    /**
     * Attempts to find a matching subscription.
     *
     * @param subscription [Subscription]
     * @return [String]? - returns the subscription id, otherwise it returns null.
     */
    override fun findSubscriptionId(subscription: Subscription): String?
        = subscriptionListCache.get(0).firstOrNull { subscription == it }?.subscriptionId

    override var healthCheckSystem = subscriptionLoaderImpl.healthCheckSystem
}