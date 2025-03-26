package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionResult
import gov.cdc.ocio.processingstatusnotifications.subscription.SubscriptionManager
import java.time.Instant
import mu.KotlinLogging


/**

 * @property logger KLogger
 * @property cacheService InMemoryCacheService
 * @constructor
 */
class UnsubscribeNotifications {

    private val logger = KotlinLogging.logger {}

    private val cacheService: SubscriptionManager = SubscriptionManager()

    /**
     * The function which validates and Unsubscribes for webhook notifications.
     *
     * @param subscriptionId String
     */
    fun run(subscriptionId: String): SubscriptionResult {
        logger.debug { "SubscriptionId $subscriptionId" }

        val result = SubscriptionResult()
        val unsubscribeSuccessful = unsubscribeNotifications(subscriptionId)
        if (subscriptionId.isNotBlank() && unsubscribeSuccessful) {
            result.subscriptionId = subscriptionId
            result.timestamp = Instant.now().epochSecond
            result.status = false
            result.message = "UnSubscription successful"

        } else {
            result.status = false
            result.message = "UnSubscription unsuccessful"

        }
        return result
    }

    /**
     * Function which unsubscribes based on subscription id from the cache service.
     *
     * @param subscriptionId String
     * @return Boolean true if successful, false otherwise
     */
    private fun unsubscribeNotifications(
        subscriptionId: String,
    ): Boolean {
        return runCatching {
            cacheService.unsubscribeNotifications(subscriptionId)
        }.isSuccess
    }
}