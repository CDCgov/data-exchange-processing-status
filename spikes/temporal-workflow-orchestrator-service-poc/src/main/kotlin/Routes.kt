@file:Suppress("PLUGIN_IS_NOT_ENABLED")
package gov.cdc.ocio.processingnotifications

import gov.cdc.ocio.processingnotifications.model.DeadlineCheckSubscription
import gov.cdc.ocio.processingnotifications.model.DeadlineCheckUnSubscription
import gov.cdc.ocio.processingnotifications.model.UploadErrorsNotificationSubscription
import gov.cdc.ocio.processingnotifications.model.UploadErrorsNotificationUnSubscription
import gov.cdc.ocio.processingnotifications.service.DeadLineCheckSubscriptionService
import gov.cdc.ocio.processingnotifications.service.DeadLineCheckUnSubscriptionService
import gov.cdc.ocio.processingnotifications.service.UploadErrorsNotificationSubscriptionService
import gov.cdc.ocio.processingnotifications.service.UploadErrorsNotificationUnSubscriptionService
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
 * Route to Unsubscribe for DeadlineCheck unsubscription
 */
fun Route.unsubscribeDeadlineCheck() {
    post("/unsubscribe/deadlineCheck") {
        val subscription = call.receive<DeadlineCheckUnSubscription>()
        val result = DeadLineCheckUnSubscriptionService().run(subscription.subscriptionId)
        call.respond(result)
    }
}


/**
 * Route to subscribe for DeadlineCheck subscription
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
 * Route to Unsubscribe for DeadlineCheck unsubscription
 */
fun Route.unsubscribeUploadErrorsNotification() {
    post("/unsubscribe/uploadErrorsNotification") {
        val subscription = call.receive<UploadErrorsNotificationUnSubscription>()
        val result = UploadErrorsNotificationUnSubscriptionService().run(subscription.subscriptionId)
        call.respond(result)
    }
}
