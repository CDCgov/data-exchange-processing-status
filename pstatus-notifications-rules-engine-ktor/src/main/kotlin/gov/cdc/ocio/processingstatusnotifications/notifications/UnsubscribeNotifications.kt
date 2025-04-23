package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionResult
import gov.cdc.ocio.processingstatusnotifications.subscription.SubscriptionManager
import jdk.jshell.spi.ExecutionControl.InternalException
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

        require(subscriptionId.isNotBlank()) { "Required field subscriptionId can not be blank" }

        if (!unsubscribeNotifications(subscriptionId))
            error("Failed to unsubscribe, check the subscriptionId is valid")

        return SubscriptionResult(subscriptionId)
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