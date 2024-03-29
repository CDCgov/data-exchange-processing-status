package gov.cdc.ocio.function.http

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
 *              dataStreamId
 *              dataStreamRoute
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

    fun run(dataStreamId: String,
        dataStreamRoute: String):
            HttpResponseMessage {
        // reading query parameters from http request for email subscription
        val url = request.queryParameters["url"]
        val stageName = request.queryParameters["stageName"]
        val statusType = request.queryParameters["statusType"]

        logger.debug("dataStreamId: $dataStreamId")
        logger.debug("dataStreamRoute: $dataStreamRoute")
        logger.debug("Subscription Websocket Url: $url")
        logger.debug("StageName: $stageName")
        logger.debug("StatusType: $statusType")

        val subscriptionResult = subscribeForEmail(dataStreamId, dataStreamRoute, url, stageName, statusType)
        return if (subscriptionResult.subscription_id != null) {
            subscriptionResult.message = "Subscription successfull"
            request.createResponseBuilder(HttpStatus.OK).body(subscriptionResult).build()
        } else {
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(subscriptionResult).build()
        }
    }

    private fun subscribeForEmail(
        dataStreamId: String,
        dataStreamRoute: String,
        url: String?,
        stageName: String?,
        statusType: String?
    ): SubscriptionResult {
        // TODO: Add logic to subscribe and return subscription id appropriately

        val result = SubscriptionResult()
        if (dataStreamId.isBlank()
            || dataStreamRoute.isBlank()
            || url.isNullOrBlank()
            || stageName.isNullOrBlank()
            || statusType.isNullOrBlank()) {
            result.status = false
            result.message = "Required fields not sent in request"
        } else if (!url.lowercase().startsWith("ws")) {
//        } else if (!url.lowercase().matches(Regex("^ws[s]*://[0-9a-zA-Z-_.]*[:]*[0-9a-zA-Z]*"))) {
            result.status = false
            result.message = "Not valid url address"
        } else if (!(statusType.equals("success", true) || statusType.equals("warning", true) || statusType.equals("failure", true))) {
            result.status = false
            result.message = "Not valid status"
        } else {
            result.subscription_id = cacheService.updateNotificationsPreferences(dataStreamId, dataStreamRoute, stageName, statusType, url, SubscriptionType.WEBSOCKET)
            result.timestamp = Instant.now().epochSecond
            result.status = true
            result.message = "Subscription for Websocket setup"
        }

        return result
    }
}