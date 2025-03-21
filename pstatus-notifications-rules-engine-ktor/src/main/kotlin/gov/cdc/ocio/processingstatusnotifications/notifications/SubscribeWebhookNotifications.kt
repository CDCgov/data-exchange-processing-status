package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.SubscriptionResult
import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionType
import gov.cdc.ocio.processingstatusnotifications.WebhookSubscription
import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingstatusnotifications.model.message.Status
import mu.KotlinLogging
import java.time.Instant


/**
 * This class is used by graphL endpoints to subscribe for Webhook notifications
 * based on rules sent in required parameters/arguments:
 *   - dataStreamId
 *   - dataStreamRoute
 *   - stage info, namely the stage's "service" and "action"
 *   - status ("success", "failure")
 *   - url (websocket url)
 * @property logger KLogger
 * @property cacheService InMemoryCacheService
 */
class SubscribeWebhookNotifications {
    private val logger = KotlinLogging.logger {}
    private val cacheService: InMemoryCacheService = InMemoryCacheService()

    /**
     * Validates and subscribes for webhook notifications
     *
     * @param subscription WebhookSubscription
     */
    fun run(subscription: WebhookSubscription): SubscriptionResult {

        val dataStreamId = subscription.dataStreamId
        val dataStreamRoute = subscription.dataStreamRoute
        val url = subscription.url
        val service = subscription.service
        val action = subscription.action
        val status = subscription.status

        logger.debug("dataStreamId: $dataStreamId")
        logger.debug("dataStreamRoute: $dataStreamRoute")
        logger.debug("Subscription Url: $url")
        logger.debug("service: $service, action: $action")
        logger.debug("Status: $status")

        val subscriptionResult = subscribeForWebhook(dataStreamId, dataStreamRoute, url, service, action, status)
        if (subscriptionResult.subscription_id != null) {
            subscriptionResult.message = "Subscription successful"
            return subscriptionResult
        }
        subscriptionResult.message = "Invalid Request"
        subscriptionResult.status = false

        return subscriptionResult
    }

    /**
     * Validates and updates the notification preferences of the cacheService
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param url String?
     * @param service String?
     * @param action String?
     * @param status Status
     * @return SubscriptionResult
     */
    private fun subscribeForWebhook(
        dataStreamId: String,
        dataStreamRoute: String,
        url: String?,
        service: String?,
        action: String?,
        status: Status
    ): SubscriptionResult {

        val result = SubscriptionResult()
        if (dataStreamId.isBlank() || dataStreamRoute.isBlank() || url.isNullOrBlank() || service.isNullOrBlank() || action.isNullOrBlank()) {
            result.status = false
            result.message = "Required fields not sent in request"
        } else if (!url.lowercase().startsWith("ws")) {
            result.status = false
            result.message = "Not valid url address"
        } else {
            result.subscription_id = cacheService.updateNotificationsPreferences(
                dataStreamId,
                dataStreamRoute,
                service,
                action,
                status,
                url,
                SubscriptionType.WEBHOOK
            )
            result.timestamp = Instant.now().epochSecond
            result.status = true
            result.message = "Subscription for Webhook setup"
        }

        return result
    }
}