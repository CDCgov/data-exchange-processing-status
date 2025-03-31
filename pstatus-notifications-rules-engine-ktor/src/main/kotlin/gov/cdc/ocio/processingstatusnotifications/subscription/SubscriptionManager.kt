package gov.cdc.ocio.processingstatusnotifications.subscription

import gov.cdc.ocio.processingstatusnotifications.exception.*
import gov.cdc.ocio.types.model.Notification
import gov.cdc.ocio.processingstatusnotifications.model.Subscription
import gov.cdc.ocio.types.model.SubscriptionRule
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*


/**
 * This class is a service that interacts with InMemory Cache in order to subscribe/unsubscribe users.
 */
class SubscriptionManager : KoinComponent {

    private val cachedSubscriptionLoader by inject<CachedSubscriptionLoader>()

    /**
     * Upserts a subscription.  If the subscription exists it is updated, otherwise a new subscription is created.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String?
     * @param ruleDescription String?
     * @param mvelCondition String
     * @param notification Notification
     * @return String
     */
    fun upsertSubscription(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String?,
        ruleDescription: String?,
        mvelCondition: String,
        notification: Notification
    ): String {
        try {
            // Create the subscription rule
            val subscriptionRule = SubscriptionRule(
                dataStreamId,
                dataStreamRoute,
                jurisdiction,
                ruleDescription,
                mvelCondition
            )

            val newSubscriptionId = UUID.randomUUID().toString()

            // Create the subscription
            val subscription = Subscription(
                newSubscriptionId,
                subscriptionRule,
                notification
            )

            // Check if the subscription id already exists and if so, get the subscription id so we can replace it.
            // Otherwise, generate a new subscription id.
            val subscriptionId = cachedSubscriptionLoader.findSubscriptionId(subscription)
                ?: newSubscriptionId

            // Add/replace the subscription
            cachedSubscriptionLoader.upsertSubscription(subscriptionId, subscription)

            return subscriptionId
        } catch (e: BadStateException) {
            throw e
        }
    }

    /**
     * Attempts to remove the subscription with the provided subscription id.
     *
     * @param subscriptionId String
     * @return Boolean
     * @throws BadStateException thrown if subscription not found
     */
    @Throws(BadStateException::class)
    fun unsubscribeNotifications(subscriptionId: String) {
        cachedSubscriptionLoader.removeSubscription(subscriptionId).also {
            if (!it) throw BadStateException("Subscription id provided not found")
        }
    }

    /**
     * Returns the subscription for the subscriptionId provided.
     *
     * @param subscriptionId String
     * @return Subscription?
     */
    fun getSubscription(subscriptionId: String) = cachedSubscriptionLoader.getSubscription(subscriptionId)

    /**
     * Returns all the subscriptions.
     *
     * @return List<Subscription>
     */
    fun getSubscriptions() = cachedSubscriptionLoader.getSubscriptions()
}
