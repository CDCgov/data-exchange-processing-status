package gov.cdc.ocio.functions.http

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import java.util.*
import com.microsoft.azure.functions.HttpResponseMessage
import gov.cdc.ocio.cache.InMemoryCacheService
import gov.cdc.ocio.model.http.SubscriptionResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

/**
 * This method is used by HTTP endpoints to unsubscribe for any notifications
 * by passing required parameter of subscriptionId
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property logger KLogger
 * @property cacheService InMemoryCacheService
 * @constructor
 */
class UnsubscribeNotifications(
    private val request: HttpRequestMessage<Optional<String>>
) {
    private val logger = KotlinLogging.logger {}
    private val cacheService: InMemoryCacheService = InMemoryCacheService()

    fun run(subscriptionId: String): HttpResponseMessage {
        logger.debug { "SubscriptionId $subscriptionId" }

        val result = SubscriptionResult()
        val unsubscribeSuccessfull = unsubscribeNotifications(subscriptionId)
        return if (subscriptionId.isNotBlank() && unsubscribeSuccessfull) {
            result.subscription_id = subscriptionId
            result.timestamp = Instant.now().epochSecond
            result.status = false
            result.message = "Unsubscription successfull"
            request.createResponseBuilder(HttpStatus.OK).body(result).build()
        } else {
            result.status = false
            result.message = "Unsubscription unsuccessfull"
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(result).build()
        }
    }

    private fun unsubscribeNotifications(
        subscriptionId: String,
    ): Boolean {
        return cacheService.unsubscribeNotifications(subscriptionId)
    }
}