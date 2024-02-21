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
 * This method is used by HTTP endpoints to subscribe for Websocket notifications
 * based on rules sent in required parameters/arguments
 *              destinationId
 *              eventType
 *              stageName
 *              statusType ("warning", "success", "error")
 *              url (websocket url)
 *
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property logger KLogger
 * @property cacheService InMemoryCacheService
 * @constructor
 */
class SubscribeWebsocketNotifications(
    private val request: HttpRequestMessage<Optional<String>>) {
    private val logger = KotlinLogging.logger {}
    private val cacheService: InMemoryCacheService = InMemoryCacheService()

    fun run(destinationId: String,
        eventType: String):
            HttpResponseMessage {
        // reading query parameters from http request for email subscription
        val url = request.queryParameters["url"]
        val stageName = request.queryParameters["stageName"]
        val statusType = request.queryParameters["statusType"]

        logger.info("DestinationId: $destinationId")
        logger.info("EventType: $eventType")
        logger.info("Subscription Websocket Url: $url")
        logger.info("StageName: $stageName")
        logger.info("StatusType: $statusType")

        val subscriptionResult = subscribeForEmail(destinationId, eventType, url, stageName, statusType)
        return if (subscriptionResult.subscription_id != null) {
            subscriptionResult.message = "Subscription successfull"
            request.createResponseBuilder(HttpStatus.OK).body(subscriptionResult).build()
        } else {
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(subscriptionResult).build()
        }
    }

    private fun subscribeForEmail(
        destinationId: String,
        eventType: String,
        url: String?,
        stageName: String?,
        statusType: String?
    ): SubscriptionResult {
        // TODO: Add logic to subscribe and return subscription id appropriately

        val result = SubscriptionResult()
        if (destinationId.isBlank()
            || eventType.isBlank()
            || url.isNullOrBlank()
            || stageName.isNullOrBlank()
            || statusType.isNullOrBlank()) {
            result.status = false
            result.message = "Required fields not sent in request"
        } else if (!url.lowercase().startsWith("ws")) {
//        } else if (!url.lowercase().matches(Regex("^ws[s]*://[0-9a-zA-Z-_.]*[:]*[0-9a-zA-Z]*"))) {
            result.status = false
            result.message = "Not valid url address"
        } else if (!(statusType.equals("success", true) || statusType.equals("warning", true) || statusType.equals("error", true))) {
            result.status = false
            result.message = "Not valid status"
        } else {
            result.subscription_id = cacheService.updateNotificationsPreferences(destinationId, eventType, stageName, statusType, url, SubscriptionType.WEBSOCKET)
            result.timestamp = Instant.now().epochSecond
            result.status = true
            result.message = "Subscription for Websocket setup"
        }

        return result
    }
}