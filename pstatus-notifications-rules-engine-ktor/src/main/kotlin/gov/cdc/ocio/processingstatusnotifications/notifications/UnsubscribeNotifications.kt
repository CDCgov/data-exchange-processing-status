package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.SubscriptionResult
import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCacheService
import java.time.Instant
import mu.KotlinLogging


/**

 * @property logger KLogger
 * @property cacheService InMemoryCacheService
 * @constructor
 */
class UnSubscribeNotifications {

    private val logger = KotlinLogging.logger {}

    private val cacheService: InMemoryCacheService = InMemoryCacheService()

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
            result.subscription_id = subscriptionId
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
     */
    private fun unsubscribeNotifications(
        subscriptionId: String,
    ): Boolean {
        return cacheService.unsubscribeNotifications(subscriptionId)
    }
}