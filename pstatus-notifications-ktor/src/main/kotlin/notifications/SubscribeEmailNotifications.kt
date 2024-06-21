package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.EmailSubscription
import gov.cdc.ocio.processingstatusnotifications.SubscriptionResult
import gov.cdc.ocio.processingstatusnotifications.SubscriptionType
import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCacheService
import mu.KotlinLogging
import java.time.Instant

class SubscribeEmailNotifications(){
    private val logger = KotlinLogging.logger {}
    private val cacheService: InMemoryCacheService = InMemoryCacheService()

    fun run(subscription: EmailSubscription):
            SubscriptionResult {
        val dataStreamId = subscription.dataStreamId
        val dataStreamRoute = subscription.dataStreamRoute
        val email = subscription.email
        val stageName = subscription.stageName
        val statusType = subscription.statusType

        logger.debug("dataStreamId: $dataStreamId")
        logger.debug("dataStreamRoute: $dataStreamRoute")
        logger.debug("Subscription Email Id: $email")
        logger.debug("StageName: $stageName")
        logger.debug("StatusType: $statusType")

        val subscriptionResult = subscribeForEmail(dataStreamId, dataStreamRoute, email, stageName, statusType)
            if (subscriptionResult.subscription_id == null) {
                subscriptionResult.message = "Invalid Request"
                subscriptionResult.status = false
            }
        return subscriptionResult
    }

    private fun subscribeForEmail(
        dataStreamId: String,
        dataStreamRoute: String,
        email: String?,
        stageName: String?,
        statusType: String?
    ): SubscriptionResult {
        val result = SubscriptionResult()
        if (dataStreamId.isBlank()
            || dataStreamRoute.isBlank()
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
            result.subscription_id = cacheService.updateNotificationsPreferences(dataStreamId, dataStreamRoute, stageName, statusType, email,
                SubscriptionType.EMAIL
            )
            result.timestamp = Instant.now().epochSecond
            result.status = true
            result.message = "Subscription for Email setup"
        }

        return result
    }
}