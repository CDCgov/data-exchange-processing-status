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
import java.time.Instant


class SubscribeEmailNotifications {

    fun run(
        @HttpTrigger(name="req",
                methods = [HttpMethod.POST],
                authLevel = AuthorizationLevel.ANONYMOUS)
        request: HttpRequestMessage<Optional<String>>,
        destinationId: String,
        eventType: String,
        context: ExecutionContext):
            HttpResponseMessage {

        val logger = context.logger

        // reading query parameters from http request for email subscription
        val email = request.queryParameters["email"]
        val stageName = request.queryParameters["stageName"]
        val statusType = request.queryParameters["statusType"]

        logger.info("DestinationId: $destinationId")
        logger.info("EventType: $eventType")
        logger.info("Subscription Email Id: $email")
        logger.info("StageName: $stageName")
        logger.info("StatusType: $statusType")

        val subscriptionResult = subscribeForEmail(destinationId, eventType, email, stageName, statusType)
        return if (subscriptionResult.subscription_id != null) {
            subscriptionResult.message = "Subscription successfull"
            request.createResponseBuilder(HttpStatus.OK).body(subscriptionResult).build()
        } else {
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(subscriptionResult).build();
        }
    }

    private fun subscribeForEmail(
        destinationId: String,
        eventType: String,
        email: String?,
        stageName: String?,
        statusType: String?
    ): SubscriptionResult {
        // TODO: Add logic to subscribe and return subscription id appropriately

        val result = SubscriptionResult()
        if (destinationId.isBlank()
            || eventType.isBlank()
            || email.isNullOrBlank()
            || stageName.isNullOrBlank()
            || statusType.isNullOrBlank()) {
            result.status = false
            result.message = "Required fields not sent in request"
        } else if (!email.contains('@') || email.split(".").size > 2 || !email.matches(Regex("([a-zA-Z0-9_%-]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,})\$"))) {
            result.status = false
            result.message = "Not valid email address"
        } else if (!(statusType == "success" || statusType == "warning" || statusType == "error")) {
            result.status = false
            result.message = "Not valid email address"
        } else {
            result.subscription_id = UUID.randomUUID()
            result.timestamp = Instant.now().epochSecond
            result.status = true
            result.message = "Subscription for Email setup"
        }

        return result;
    }
}