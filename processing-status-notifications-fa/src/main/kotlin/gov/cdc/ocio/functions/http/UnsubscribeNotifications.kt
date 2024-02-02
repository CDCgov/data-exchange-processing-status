package gov.cdc.ocio.functions.http

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import java.util.*
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.ocio.cache.InMemoryCacheService
import gov.cdc.ocio.model.http.SubscriptionResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

class UnsubscribeNotifications(
    private val request: HttpRequestMessage<Optional<String>>) {
    private val logger = KotlinLogging.logger {}
    private val cacheService: InMemoryCacheService = InMemoryCacheService()

    fun run(
        @HttpTrigger(name="req",
                methods = [HttpMethod.PUT],
                authLevel = AuthorizationLevel.ANONYMOUS)
        notificationType: String,
        subscriptionId: String):
            HttpResponseMessage {

        logger.info("NotificationType $notificationType")
        logger.info("SubscriptionId $subscriptionId")

        val result = SubscriptionResult()
        val unsubscribeSuccessfull = unsubscribeNotifications(notificationType, subscriptionId)
        return if (subscriptionId.isNotBlank() && unsubscribeSuccessfull) {
            result.subscription_id = subscriptionId
            result.timestamp = Instant.now().epochSecond
            result.status = false
            result.message = "Unsubscription successfull for $notificationType"
            request.createResponseBuilder(HttpStatus.OK).body(result).build()
        } else {
            result.status = false
            result.message = "Unsubscription unsuccessfull for $notificationType"
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(result).build();
        }
    }

    private fun unsubscribeNotifications(
        notificationType: String, // TODO: this feels redundant, we dont need this. Remove this
        subscriptionId: String,
    ): Boolean {
        return cacheService.unsubscribeNotifications(subscriptionId);
    }
}