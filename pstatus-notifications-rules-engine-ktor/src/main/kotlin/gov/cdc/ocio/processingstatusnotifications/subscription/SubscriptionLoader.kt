package gov.cdc.ocio.processingstatusnotifications.subscription

import gov.cdc.ocio.processingstatusnotifications.model.Subscription
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * The interface which loads notification subscriptions.
 */
interface SubscriptionLoader {

    val system: String
        get() = "Subscription Loader"

    /**
     * Defines the interface for retrieving all the notification subscriptions.
     *
     * @return [List]<[Subscription]>
     */
    fun getSubscriptions(): List<Subscription>

    /**
     * Get the subscription associated with the subscription id provided.
     *
     * @param subscriptionId [String]
     * @return [Subscription]
     */
    fun getSubscription(subscriptionId: String): Subscription

    /**
     * Upserts a subscription -- if it does not exist it is added, otherwise the subscription is replaced.
     *
     * @param subscriptionId String
     * @param subscription Subscription
     * @return Boolean - true if successful, false otherwise
     */
    fun upsertSubscription(subscriptionId: String, subscription: Subscription): Boolean

    /**
     * Removes the subscription associated with the subscription id provided.
     *
     * @param subscriptionId String
     * @return Boolean - true if successful, false otherwise
     */
    fun removeSubscription(subscriptionId: String): Boolean

    /**
     * Attempts to find a matching subscription.
     *
     * @param subscription [Subscription]
     * @return [String]? - returns the subscription id, otherwise it returns null.
     */
    fun findSubscriptionId(subscription: Subscription): String?

//    var healthCheckSystem: HealthCheckSystem
}
