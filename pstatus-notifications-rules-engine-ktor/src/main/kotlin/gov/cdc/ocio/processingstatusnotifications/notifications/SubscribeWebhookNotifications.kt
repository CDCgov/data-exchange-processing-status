package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.subscription.SubscriptionManager
import gov.cdc.ocio.processingstatusnotifications.model.*
import gov.cdc.ocio.types.model.WebhookNotification
import jdk.jshell.spi.ExecutionControl.InternalException
import mu.KotlinLogging


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
        val ruleDescription = subscription.ruleDescription
        val mvelCondition = subscription.mvelCondition
        val webhookUrl = subscription.webhookUrl

        require(dataStreamId.isNotBlank()) { "Required field dataStreamId can not be blank" }
        require(dataStreamRoute.isNotBlank()) { "Required field dataStreamRoute can not be blank" }
        require(mvelCondition.isNotBlank()) { "Required field mvelCondition can not be blank" }
        require(webhookUrl.isNotBlank()) { "Required field webhookUrl can not be blank" }

        require(webhookUrl.startsWith("http://", ignoreCase = true) ||
                webhookUrl.startsWith("https://", ignoreCase = true)) {
            "Invalid webhook URL: must begin with http:// or https://"
        }

        val subscriptionResult = SubscriptionResult(
            subscriptionId = cacheService.upsertSubscription(
                dataStreamId,
                dataStreamRoute,
                jurisdiction,
                ruleDescription,
                mvelCondition,
                WebhookNotification(webhookUrl)
            )
        )

        if (subscriptionResult.subscriptionId == null)
            throw InternalException("Unable to setup the subscription")

        return subscriptionResult
    }
}