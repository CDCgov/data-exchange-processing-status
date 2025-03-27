package gov.cdc.ocio.processingstatusnotifications.subscription

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusnotifications.model.Subscription
import io.ktor.server.plugins.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Database backed loader for the notification subscriptions.
 *
 * @property repository ProcessingStatusRepository
 * @property notificationSubscriptions Collection
 * @property cName String
 * @property cVar String
 */
class DatabaseSubscriptionLoader: KoinComponent, SubscriptionLoader {

    private val repository by inject<ProcessingStatusRepository>()

    private val notificationSubscriptions = repository.notificationSubscriptionsCollection

    private val cName = notificationSubscriptions.collectionNameForQuery
    private val cVar = notificationSubscriptions.collectionVariable

    /**
     * Defines the interface for retrieving all the notification subscriptions.
     *
     * @return [List]<[Subscription]>
     */
    override fun getSubscriptions(): List<Subscription> {
        return notificationSubscriptions.queryItems("select * from $cName $cVar", Subscription::class.java)
    }

    /**
     * Get the subscription associated with the subscription id provided.
     *
     * @param subscriptionId [String]
     * @return [Subscription]
     */
    override fun getSubscription(subscriptionId: String): Subscription {
        return notificationSubscriptions.getItem(subscriptionId, Subscription::class.java)
            ?: throw NotFoundException("Subscription id $subscriptionId not found")
    }

    /**
     * Upserts a subscription -- if it does not exist it is added, otherwise the subscription is replaced.
     *
     * @param subscriptionId String
     * @param subscription Subscription
     * @return Boolean - true if successful, false otherwise
     */
    override fun upsertSubscription(subscriptionId: String, subscription: Subscription): Boolean {
        // Write the subscription to the repository
        return notificationSubscriptions.createItem(subscriptionId, subscription, Subscription::class.java, subscriptionId)
    }

    /**
     * Removes the subscription associated with the subscription id provided.
     *
     * @param subscriptionId String
     * @return Boolean - true if successful, false otherwise
     */
    override fun removeSubscription(subscriptionId: String): Boolean {
        return notificationSubscriptions.deleteItem(subscriptionId, subscriptionId)
    }

    /**
     * Attempts to find a matching subscription.
     *
     * @param subscription [Subscription]
     * @return [String]? - returns the subscription id, otherwise it returns null.
     */
    override fun findSubscriptionId(subscription: Subscription): String? {
        throw NotImplementedError("Should never be called, caching should handle this.")
    }

//    override var healthCheckSystem: HealthCheckSystem
//        get() = TODO("Not yet implemented")
//        set(value) {}

//    override var healthCheckSystem = HealthCheckDatabase(system, schemaLocalSystemFilePath) as HealthCheckSystem
}