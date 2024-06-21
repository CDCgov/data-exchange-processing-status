@file:Suppress("PLUGIN_IS_NOT_ENABLED")

package gov.cdc.ocio.processingstatusnotifications

import gov.cdc.ocio.processingstatusnotifications.notifications.UnSubscribeNotifications
import gov.cdc.ocio.processingstatusnotifications.notifications.SubscribeEmailNotifications
import gov.cdc.ocio.processingstatusnotifications.notifications.SubscribeWebhookNotifications
import kotlinx.serialization.Serializable
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


/**
 * Email Subscription data class which is serialized back and forth
 */
@Serializable
data class EmailSubscription(val dataStreamId:String,
                             val dataStreamRoute:String,
                             val email: String,
                             val stageName: String,
                             val statusType:String)
/**
 * Webhook Subscription data class which is serialized back and forth
 */
@Serializable
data class WebhookSubscription(val dataStreamId:String,
                             val dataStreamRoute:String,
                             val url: String,
                             val stageName: String,
                             val statusType:String)

/**
 * UnSubscription data class which is serialized back and forth when we need to unsubscribe by the subscriptionId
 */
@Serializable
data class UnSubscription(val subscriptionId:String)

/**
 * The resultant class for subscription of email/webhooks
 */
@Serializable
data class SubscriptionResult(
    var subscription_id: String? = null,
    var timestamp: Long? = null,
    var status: Boolean? = false,
    var message: String? = ""
)

/**
 * Route to subscribe for email notifications
 */
fun Route.subscribeEmailNotificationRoute() {
    post("/subscribe/email") {
        val subscription = call.receive<EmailSubscription>()
        val emailSubscription =EmailSubscription(subscription.dataStreamId, subscription.dataStreamRoute, subscription.email, subscription.stageName, subscription.statusType)
        val result = SubscribeEmailNotifications().run(emailSubscription);
        call.respond(result)

    }
}
/**
 * Route to Unsubscribe for email notifications
 */
fun Route.unsubscribeEmailNotificationRoute() {
    post("/unsubscribe/email") {
        val subscription = call.receive<UnSubscription>()
        val result = UnSubscribeNotifications().run(subscription.subscriptionId);
        call.respond(result)
    }
}
/**
 * Route to subscribe for webhook notifications
 */
fun Route.subscribeWebhookRoute() {
    post("/subscribe/webhook") {
        val subscription = call.receive<WebhookSubscription>()
        val webhookSubscription = WebhookSubscription(subscription.dataStreamId, subscription.dataStreamRoute, subscription.url, subscription.stageName, subscription.statusType)
        val result = SubscribeWebhookNotifications().run(webhookSubscription);
        call.respond(result)

    }
}
/**
 * Route to unsubscribe for webhook notifications
 */
fun Route.unsubscribeWebhookRoute() {
    post("/unsubscribe/webhook") {
        val subscription = call.receive<UnSubscription>()
        val result = UnSubscribeNotifications().run(subscription.subscriptionId);
        call.respond(result)
    }
}