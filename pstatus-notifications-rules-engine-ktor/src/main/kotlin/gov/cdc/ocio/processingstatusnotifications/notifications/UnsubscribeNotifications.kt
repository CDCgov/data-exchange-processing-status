package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionResult
import gov.cdc.ocio.processingstatusnotifications.subscription.SubscriptionManager
import mu.KotlinLogging


/**

 * @property logger KLogger
 * @property cacheService InMemoryCacheService
 * @constructor
 */
class UnsubscribeNotifications {

    private val logger = KotlinLogging.logger {}

    private val cacheService = SubscriptionManager()

    /**
     * The function which validates and Unsubscribes for webhook notifications.
     *
     * @param subscriptionId String
     */
    fun run(subscriptionId: String): SubscriptionResult {
        logger.debug { "Received request to unsubscribe from subscriptionId $subscriptionId" }

        val result = SubscriptionResult().apply {
            this.subscriptionId = subscriptionId
        }
        if (subscriptionId.isNotBlank() && unsubscribeNotifications(subscriptionId)) {
            result.status = true
            result.message = "Successfully unsubscribed"
        } else {
            result.status = false
            result.message = "Failed to unsubscribe"
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