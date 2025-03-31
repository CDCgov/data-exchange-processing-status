package gov.cdc.ocio.processingstatusnotifications.notifications

import gov.cdc.ocio.processingstatusnotifications.model.EmailSubscription
import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionResult
import gov.cdc.ocio.processingstatusnotifications.subscription.SubscriptionManager
import gov.cdc.ocio.types.model.EmailNotification
import java.time.Instant


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
        if (dataStreamId.isBlank()
            || dataStreamRoute.isBlank()
            || mvelCondition.isBlank()
            || emailAddresses.isEmpty()) {
            return SubscriptionResult(
                status = false,
                message = "Required fields not sent in request"
            )
        }

        // Validate the email address.
        for (email in emailAddresses) {
            if (!email.contains('@') || email.split(".").size > 2
                || !email.matches(Regex("([a-zA-Z0-9_%-]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,})\$"))) {
                return SubscriptionResult(
                    status = false,
                    message = "'$email' is not a valid email address"
                )
            }
        }

        val subscriptionResult = SubscriptionResult(
            status = true,
            subscriptionId = cacheService.upsertSubscription(
                dataStreamId,
                dataStreamRoute,
                jurisdiction,
                ruleDescription,
                mvelCondition,
                EmailNotification(emailAddresses)
            ),
            timestamp = Instant.now().epochSecond,
            message = "Subscription for email setup"
        )

        if (subscriptionResult.subscriptionId == null) {
            subscriptionResult.status = false
            subscriptionResult.message = "Invalid Request"
        }

        return subscriptionResult
    }
}