package gov.cdc.ocio.processingstatusnotifications

import gov.cdc.ocio.processingstatusnotifications.email.UnsubscribeEmailNotifications
import kotlinx.serialization.Serializable
import io.ktor.server.plugins.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*


@Serializable
data class EmailSubscription(val dataStreamId:String, val dataStreamRoute:String,val email: String, val stageName: String, val statusType:String, val subscriptionId:String)

fun Route.subscribeEmailNotificationRoute() {
    post("subscribe/email/{dataStreamId}/{dataStreamRoute}") {
        val subscription = call.receive<EmailSubscription>()
        val result = SubscribeEmailNotifications().run(subscription);
        if (result.subscription_id!=null) {
            call.respond("Subscription successful!")
        } else {
            call.respond("Failed to send subscription email.")
        }
    }
}

fun Route.unsubscribeEmailNotificationRoute() {
    post("unsubscribe/{subscriptionId}") {
        val subscription = call.receive<EmailSubscription>()
        val result = UnsubscribeEmailNotifications().run(subscription.subscriptionId);
        if (result.subscription_id!=null) {
            call.respond("Unsubscription successful")
        } else {
            call.respond("Unsubscription unsuccessful")
        }
    }
}

