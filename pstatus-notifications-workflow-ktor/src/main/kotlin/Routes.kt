@file:Suppress("PLUGIN_IS_NOT_ENABLED")
package gov.cdc.ocio.processingnotifications

import gov.cdc.ocio.processingnotifications.model.*
import gov.cdc.ocio.processingnotifications.service.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Route to subscribe for DeadlineCheck subscription
 */
fun Route.subscribeDeadlineCheckRoute() {
    post("/subscribe/deadlineCheck") {
        val subscription = call.receive<DeadlineCheckSubscription>()
        val deadlineCheckSubscription = DeadlineCheckSubscription(subscription.dataStreamId, subscription.dataStreamRoute, subscription.jurisdiction,
            subscription.daysToRun, subscription.timeToRun, subscription.deliveryReference)
        val result = DeadLineCheckSubscriptionService().run(deadlineCheckSubscription)
        call.respond(result)

    }
}
/**
 * Route to unsubscribe for DeadlineCheck subscription
 */
fun Route.unsubscribeDeadlineCheck() {
    post("/unsubscribe/deadlineCheck") {
        val subscription = call.receive<DeadlineCheckUnSubscription>()
        val result = DeadLineCheckUnSubscriptionService().run(subscription.subscriptionId)
        call.respond(result)
    }
}


/**
 * Route to subscribe for upload errors notification subscription
 */
fun Route.subscribeUploadErrorsNotification() {
    post("/subscribe/uploadErrorsNotification") {
        val subscription = call.receive<UploadErrorsNotificationSubscription>()
        val uploadErrorsNotificationSubscription = UploadErrorsNotificationSubscription(subscription.dataStreamId, subscription.dataStreamRoute,
            subscription.jurisdiction,
            subscription.daysToRun, subscription.timeToRun,  subscription.deliveryReference)
        val result = UploadErrorsNotificationSubscriptionService().run(uploadErrorsNotificationSubscription)
        call.respond(result)

    }
}
/**
 * Route to unsubscribe for upload errors subscription notification
 */
fun Route.unsubscribeUploadErrorsNotification() {
    post("/unsubscribe/uploadErrorsNotification") {
        val subscription = call.receive<UploadErrorsNotificationUnSubscription>()
        val result = UploadErrorsNotificationUnSubscriptionService().run(subscription.subscriptionId)
        call.respond(result)
    }
}
/**
 * Route to subscribe for top data stream errors notification subscription
 */
fun Route.subscribeDataStreamTopErrorsNotification() {
    post("/subscribe/dataStreamTopErrorsNotification") {
        val subscription = call.receive<DataStreamTopErrorsNotificationSubscription>()
        val dataStreamTopErrorsNotificationSubscription = DataStreamTopErrorsNotificationSubscription(subscription.dataStreamId, subscription.dataStreamRoute,
            subscription.jurisdiction,
            subscription.daysToRun, subscription.timeToRun,  subscription.deliveryReference)
        val result = DataStreamTopErrorsNotificationSubscriptionService().run(dataStreamTopErrorsNotificationSubscription)
        call.respond(result)

    }
}
/**
 * Route to unsubscribe for top data stream errors notification subscription
 */
fun Route.unsubscribesDataStreamTopErrorsNotification() {
    post("/unsubscribe/dataStreamTopErrorsNotification") {
        val subscription = call.receive<DataStreamTopErrorsNotificationUnSubscription>()
        val result = DataStreamTopErrorsNotificationUnSubscriptionService().run(subscription.subscriptionId)
        call.respond(result)
    }
}

/**
   Route to subscribe for Temporal Server health check
 */
fun Route.healthCheckRoute() {
    get("/health") {
        call.respond(TemporalHealthCheckService().getHealth())
    }
}