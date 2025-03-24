package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.model.EmailSubscription
import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionResult
import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionType
import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingstatusnotifications.model.message.Status
import mu.KotlinLogging
import java.time.Instant


/**
 * This method is used by graphL endpoints to subscribe for Webhook notifications based on rules sent in required
 * parameters/arguments:
 *   - dataStreamId
 *   - dataStreamRoute
 *   - email
 *   - stage info, namely the stage's "service" and "action"
 *   - status ("success", "failure")
 */
class SubscribeEmailNotifications{
    private val logger = KotlinLogging.logger {}
    private val cacheService = InMemoryCacheService()

    /**
     * The function which validates and subscribes for email notifications
     * @param subscription EmailSubscription
     */
    fun run(
        subscription: EmailSubscription
    ): SubscriptionResult {
        val dataStreamId = subscription.dataStreamId
        val dataStreamRoute = subscription.dataStreamRoute
        val email = subscription.email
        val service = subscription.service
        val action = subscription.action
        val status = subscription.status

        logger.debug("dataStreamId: $dataStreamId")
        logger.debug("dataStreamRoute: $dataStreamRoute")
        logger.debug("Subscription Email Id: $email")
        logger.debug("service: $service, action: $action")
        logger.debug("StatusType: $status")

        val subscriptionResult = subscribeForEmail(
            dataStreamId,
            dataStreamRoute,
            email,
            service,
            action,
            status
        )
            if (subscriptionResult.subscriptionId == null) {
                subscriptionResult.message = "Invalid Request"
                subscriptionResult.status = false
            }
        return subscriptionResult
    }

    /**
     * This function validates and updates the notification preferences of the cacheService
     *  @param dataStreamId String
     *  @param dataStreamRoute String
     *  @param email String
     *  @param stageName String
     *  @param statusType String
     */
    private fun subscribeForEmail(
        dataStreamId: String,
        dataStreamRoute: String,
        email: String?,
        service: String?,
        action: String?,
        status: Status?
    ): SubscriptionResult {
        val result = SubscriptionResult()
        if (dataStreamId.isBlank()
            || dataStreamRoute.isBlank()
            || email.isNullOrBlank()
            || service.isNullOrBlank()
            || action.isNullOrBlank()
            || status == null) {
            result.status = false
            result.message = "Required fields not sent in request"
        } else if (!email.contains('@') || email.split(".").size > 2 || !email.matches(Regex("([a-zA-Z0-9_%-]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,})\$"))) {
            result.status = false
            result.message = "Not valid email address"
        } else if (!(status == Status.SUCCESS || status == Status.FAILURE)) {
            result.status = false
            result.message = "Not valid status"
        } else {
            result.subscriptionId = cacheService.updateNotificationsPreferences(
                dataStreamId,
                dataStreamRoute,
                service,
                action,
                status,
                email,
                SubscriptionType.EMAIL
            )
            result.timestamp = Instant.now().epochSecond
            result.status = true
            result.message = "Subscription for Email setup"
        }

        return result
    }
}