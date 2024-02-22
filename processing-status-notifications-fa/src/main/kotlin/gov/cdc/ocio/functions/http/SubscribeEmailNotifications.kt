package gov.cdc.ocio.functions.http

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import java.util.*
import com.microsoft.azure.functions.HttpResponseMessage
import gov.cdc.ocio.cache.InMemoryCacheService
import gov.cdc.ocio.model.http.SubscriptionResult
import gov.cdc.ocio.model.http.SubscriptionType
import mu.KotlinLogging
import java.time.Instant

/**
 * This method is used by HTTP endpoints to subscribe for Email notifications
 * based on rules sent in required parameters/arguments
 *              destinationId
 *              eventType
 *              stageName
 *              statusType ("warning", "success", "error")
 *              email (email id of subscriber)
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property logger KLogger
 * @property cacheService InMemoryCacheService
 * @constructor
 */
class SubscribeEmailNotifications(
    private val request: HttpRequestMessage<Optional<String>>) {
    private val logger = KotlinLogging.logger {}
    private val cacheService: InMemoryCacheService = InMemoryCacheService()
    fun run(destinationId: String,
        eventType: String):
            HttpResponseMessage {
        // reading query parameters from http request for email subscription
        val email = request.queryParameters["email"]
        val stageName = request.queryParameters["stageName"]
        val statusType = request.queryParameters["statusType"]

        logger.debug("DestinationId: $destinationId")
        logger.debug("EventType: $eventType")
        logger.debug("Subscription Email Id: $email")
        logger.debug("StageName: $stageName")
        logger.debug("StatusType: $statusType")

        var subscriptionResult = SubscriptionResult()
        if (!(email == null || stageName == null || statusType == null)) {
            subscriptionResult = subscribeForEmail(destinationId, eventType, email, stageName, statusType)
            if (subscriptionResult.subscription_id != null) {
                return request.createResponseBuilder(HttpStatus.OK).body(subscriptionResult).build()
            }
        }
        subscriptionResult.message = "Invalid Request"
        subscriptionResult.status = false
        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(subscriptionResult).build()
    }

    private fun subscribeForEmail(
        destinationId: String,
        eventType: String,
        email: String?,
        stageName: String?,
        statusType: String?
    ): SubscriptionResult {
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
            result.subscription_id = cacheService.updateNotificationsPreferences(destinationId, eventType, stageName, statusType, email, SubscriptionType.EMAIL)
            result.timestamp = Instant.now().epochSecond
            result.status = true
            result.message = "Subscription for Email setup"
        }

        return result
    }
}