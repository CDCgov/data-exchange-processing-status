package gov.cdc.ocio.processingstatusnotifications.subscription

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusnotifications.model.Subscription
import gov.cdc.ocio.types.health.HealthCheckSystem
import io.ktor.server.plugins.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class DatabaseSubscriptionLoader: KoinComponent, SubscriptionLoader {

    private val repository by inject<ProcessingStatusRepository>()

    private val notificationSubscriptions = repository.notificationSubscriptionsCollection

    private val cName = notificationSubscriptions.collectionNameForQuery
    private val cVar = notificationSubscriptions.collectionVariable

    override fun getSubscriptions(): List<Subscription> {
        return notificationSubscriptions.queryItems("select * from $cName $cVar", Subscription::class.java)
    }

    override fun getSubscription(subscriptionId: String): Subscription {
        return notificationSubscriptions.getItem(subscriptionId, Subscription::class.java)
            ?: throw NotFoundException("Subscription id $subscriptionId not found")
    }

    override fun upsertSubscription(subscriptionId: String, subscription: Subscription): Boolean {
        // Write the subscription to the repository
        return notificationSubscriptions.createItem(subscriptionId, subscription, Subscription::class.java, subscriptionId)
    }

    override fun removeSubscription(subscriptionId: String): Boolean {
        return notificationSubscriptions.deleteItem(subscriptionId, subscriptionId)
    }

    override fun findSubscriptionId(subscription: Subscription): String? {
        throw NotImplementedError("Should never be called, caching should handle this.")
    }

//    override var healthCheckSystem: HealthCheckSystem
//        get() = TODO("Not yet implemented")
//        set(value) {}

//    override var healthCheckSystem = HealthCheckDatabase(system, schemaLocalSystemFilePath) as HealthCheckSystem
}