package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.model.EmailSubscription
import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionResult
import gov.cdc.ocio.processingstatusnotifications.subscription.SubscriptionManager
import gov.cdc.ocio.types.model.EmailNotification


/**
 * This class is used by graphQL endpoints to subscribe for webhook notifications.
 */
class SubscribeEmailNotifications {

    private val cacheService = SubscriptionManager()

    /**
     * Validates and subscribes for email notifications
     *
     * @param subscription EmailSubscription
     */
    fun run(
        subscription: EmailSubscription
    ): SubscriptionResult {
        val dataStreamId = subscription.dataStreamId
        val dataStreamRoute = subscription.dataStreamRoute
        val jurisdiction = subscription.jurisdiction
        val ruleDescription = subscription.ruleDescription
        val mvelCondition = subscription.mvelCondition
        val emailAddresses = subscription.emailAddresses

        // Make sure required parameters are not empty or missing.
        require(dataStreamId.isNotBlank()) { "Required field dataStreamId can not be blank" }
        require(dataStreamRoute.isNotBlank()) { "Required field dataStreamRoute can not be blank" }
        require(mvelCondition.isNotBlank()) { "Required field mvelCondition can not be blank" }
        require(emailAddresses.isNotEmpty()) { "Required field emailAddresses can not be empty" }

        // Validate the email address.
        for (email in emailAddresses) {
            val isInvalidEmailAddress = !email.contains('@') || email.split(".").size > 2
                    || !email.matches(Regex("([a-zA-Z0-9_%-]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,})\$"))
            require(!isInvalidEmailAddress) { "'$email' is not a valid email address" }
        }

        val subscriptionResult = SubscriptionResult(
            subscriptionId = cacheService.upsertSubscription(
                dataStreamId,
                dataStreamRoute,
                jurisdiction,
                ruleDescription,
                mvelCondition,
                EmailNotification(emailAddresses)
            )
        )

        if (subscriptionResult.subscriptionId == null)
            throw InternalError("Unable to setup the subscription")

        return subscriptionResult
    }
}