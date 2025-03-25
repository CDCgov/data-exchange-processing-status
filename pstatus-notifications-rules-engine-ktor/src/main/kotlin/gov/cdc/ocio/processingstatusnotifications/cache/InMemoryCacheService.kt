package gov.cdc.ocio.processingstatusnotifications.cache

import gov.cdc.ocio.processingstatusnotifications.exception.*
import gov.cdc.ocio.processingstatusnotifications.model.Notification
import gov.cdc.ocio.processingstatusnotifications.model.Subscription
import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionRule
import java.util.*


/**
 * This class is a service that interacts with InMemory Cache in order to subscribe/unsubscribe users.
 */
class InMemoryCacheService {

    /**
     * Upserts a subscription.  If the subscription exists it is updated, otherwise a new subscription is created.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String?
     * @param mvelCondition String
     * @param notification Notification
     * @return String
     */
    fun upsertSubscription(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String?,
        mvelCondition: String,
        notification: Notification
    ): String {
        try {
            // Create the subscription rule
            val subscriptionRule = SubscriptionRule(
                dataStreamId,
                dataStreamRoute,
                jurisdiction,
                mvelCondition
            )

            // Create the subscription
            val subscription = Subscription(
                subscriptionRule,
                notification
            )

            // Check if the subscription id already exists and if so, get the subscription id so we can replace it.
            // Otherwise, generate a new subscription id.
            val subscriptionId = InMemoryCache.findSubscriptionId(subscription)
                ?: UUID.randomUUID().toString()

            // Add/replace the subscription
            InMemoryCache.subscribe(subscriptionId, subscription)

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
    fun unsubscribeNotifications(subscriptionId: String) = InMemoryCache.unsubscribe(subscriptionId)

    /**
     *  Checks for subscription rule and gets the subscriptionId, using the subscription id to retrieve the details.
     *
     * @param subscriptionId String
     * @return Subscription?
     */
    fun getSubscription(subscriptionId: String) = InMemoryCache.getSubscriptionDetails(subscriptionId)
}
