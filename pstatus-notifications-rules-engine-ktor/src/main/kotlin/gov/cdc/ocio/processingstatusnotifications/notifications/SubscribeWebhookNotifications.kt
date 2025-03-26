package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.subscription.SubscriptionManager
import gov.cdc.ocio.processingstatusnotifications.model.*
import gov.cdc.ocio.types.model.WebhookNotification
import mu.KotlinLogging
import java.time.Instant


/**
 * This class is used by graphQL endpoints to subscribe for webhook notifications.
 *
 * @property logger KLogger
 * @property cacheService InMemoryCacheService
 */
class SubscribeWebhookNotifications {

    private val logger = KotlinLogging.logger {}

    private val cacheService = SubscriptionManager()

    /**
     * Validates and subscribes for webhook notifications
     *
     * @param subscription WebhookSubscription
     */
    fun run(
        subscription: WebhookSubscription
    ): SubscriptionResult {
        val dataStreamId = subscription.dataStreamId
        val dataStreamRoute = subscription.dataStreamRoute
        val jurisdiction = subscription.jurisdiction
        val mvelCondition = subscription.mvelCondition
        val webhookUrl = subscription.webhookUrl

        if (dataStreamId.isBlank() || dataStreamRoute.isBlank() || mvelCondition.isBlank() || webhookUrl.isBlank()) {
            return SubscriptionResult(
                status = false,
                message = "Required fields not sent in request"
            )
        }

        if (!webhookUrl.lowercase().startsWith("http://")
            && !webhookUrl.lowercase().startsWith("https://")) {
            return SubscriptionResult(
                status = false,
                message = "Not a valid url address, url must begin with http:// or https://"
            )
        }

        val subscriptionResult = SubscriptionResult(
            status = true,
            subscriptionId = cacheService.upsertSubscription(
                dataStreamId,
                dataStreamRoute,
                jurisdiction,
                mvelCondition,
                WebhookNotification(webhookUrl)
            ),
            timestamp = Instant.now().epochSecond,
            message = "Subscription for webhook setup"
        )

        if (subscriptionResult.subscriptionId == null) {
            subscriptionResult.status = false
            subscriptionResult.message = "Invalid Request"
        }

        return subscriptionResult
    }
}