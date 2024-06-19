package gov.cdc.ocio.processingstatusnotifications.email



import java.util.*
import gov.cdc.ocio.processingstatusnotifications.SubscriptionResult
import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCacheService
import java.time.Instant
import mu.KotlinLogging
/**
 * This method is used by HTTP endpoints to unsubscribe for any notifications
 * by passing required parameter of subscriptionId
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property logger KLogger
 * @property cacheService InMemoryCacheService
 * @constructor
 */
class UnsubscribeEmailNotifications(

) {
    private val logger = KotlinLogging.logger {}
    private val cacheService: InMemoryCacheService = InMemoryCacheService()

    fun run(subscriptionId: String): SubscriptionResult {
        logger.debug { "SubscriptionId $subscriptionId" }

        val result = SubscriptionResult()
        val unsubscribeSuccessfull = unsubscribeNotifications(subscriptionId)
         if (subscriptionId.isNotBlank() && unsubscribeSuccessfull) {
            result.subscription_id = subscriptionId
            result.timestamp = Instant.now().epochSecond
            result.status = false
            result.message = "Unsubscription successful"

        } else {
            result.status = false
            result.message = "Unsubscription unsuccessful"

        }
        return  result
    }

    private fun unsubscribeNotifications(
        subscriptionId: String,
    ): Boolean {
        return cacheService.unsubscribeNotifications(subscriptionId)
    }
}