@file:Suppress("PLUGIN_IS_NOT_ENABLED")
package gov.cdc.ocio.processingnotifications

import gov.cdc.ocio.processingnotifications.service.DeadLineCheckNotificationService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


/**
 * DeadlineCheck Subscription data class which is serialized back and forth
 */
data class DeadlineCheckSubscription( val dataStreamId: String,
                                      val dataStreamRoute: String,
                                      val jurisdiction: String,
                                      val daysToRun: List<String>,
                                      val timeToRun: String,
                                      val deliveryReference: String)

/**
 * UnSubscription data class which is serialized back and forth when we need to unsubscribe by the subscriptionId
 */
data class DeadlineCheckUnSubscription(val subscriptionId:String)

/**
 * The resultant class for subscription of email/webhooks
 */
data class DeadlineCheckSubscriptionResult(
    var subscriptionId: String? = null,
    var message: String? = "",
    var deliveryReference:String
)

/**
 * Route to subscribe for email notifications
 */
fun Route.subscribeDeadlineCheckRoute() {
    post("/subscribe/deadlineCheck") {
        val subscription = call.receive<DeadlineCheckSubscription>()
        val deadlineCheckSubscription = DeadlineCheckSubscription(subscription.dataStreamId, subscription.dataStreamRoute, subscription.jurisdiction,
            subscription.daysToRun, subscription.timeToRun, subscription.deliveryReference)
        val result = DeadLineCheckNotificationService().run(deadlineCheckSubscription)
        call.respond(result)

    }
}
/**
 * Route to Unsubscribe for DeadlineCheck notifications
 */
/*fun Route.unsubscribeDeadlineCheck() {
    post("/unsubscribe/deadlineCheck") {
        val subscription = call.receive<UnSubscription>()
        val result = DeadLineCheckNotificationService().run(subscription.subscriptionId)
        call.respond(result)
    }
}*/


