package gov.cdc.ocio.functions.http

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import java.util.*
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.ocio.model.http.SubscriptionResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

class UnsubscribeNotifications {
    private val logger = KotlinLogging.logger {}

    fun run(
        @HttpTrigger(name="req",
                methods = [HttpMethod.PUT],
                authLevel = AuthorizationLevel.ANONYMOUS)
        request: HttpRequestMessage<Optional<String>>,
        notificationType: String,
        subscriptionId: String,
        context: ExecutionContext):
            HttpResponseMessage {

        val logger = context.logger

        logger.info("NotificationType $notificationType")
        logger.info("SubscriptionId $subscriptionId")

        val result = SubscriptionResult()
        val unsubscribeSuccessfull = unsubscribeNotifications(notificationType, subscriptionId)
        return if (subscriptionId.isNotBlank() && unsubscribeSuccessfull) {
            result.subscription_id = UUID.fromString(subscriptionId)
            result.timestamp = Instant.now().epochSecond
            result.status = unsubscribeSuccessfull
            result.message = "Unsubscription successfull for $notificationType"
            request.createResponseBuilder(HttpStatus.OK).body(result).build()
        } else {
            result.status = false
            result.message = "Unsubscription unsuccessfull for $notificationType"
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(result).build();
        }
    }

    private fun unsubscribeNotifications(
        notificationType: String?,
        subscriptionId: String?): Boolean {
        // TODO: Add logic to subscribe and return subscription id appropriately
        return true;
    }
}