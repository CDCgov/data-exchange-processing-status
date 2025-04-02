@file:Suppress("PLUGIN_IS_NOT_ENABLED")

package gov.cdc.ocio.processingstatusnotifications

import gov.cdc.ocio.processingstatusnotifications.health.HealthQueryService
import gov.cdc.ocio.processingstatusnotifications.model.EmailSubscription
import gov.cdc.ocio.processingstatusnotifications.model.UnsubscribeRequest
import gov.cdc.ocio.processingstatusnotifications.model.WebhookSubscription
import gov.cdc.ocio.processingstatusnotifications.notifications.UnsubscribeNotifications
import gov.cdc.ocio.processingstatusnotifications.notifications.SubscribeEmailNotifications
import gov.cdc.ocio.processingstatusnotifications.notifications.SubscribeWebhookNotifications
import gov.cdc.ocio.processingstatusnotifications.subscription.SubscriptionManager
import gov.cdc.ocio.types.health.HealthStatusType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*


/**
 * Route to get list of subscriptions
 */
fun Route.notificationSubscriptionsRoute() {
    get("/subscriptions") {
        val result = SubscriptionManager().getSubscriptions()
        call.respond(result)
    }
}

/**
 * Route to get a subscription by its id
 */
fun Route.notificationSubscriptionRoute() {
    get("/subscription/{id}") {
        call.parameters["id"]?.let {
            call.respond(SubscriptionManager().getSubscription(it))
        } ?: call.respond(HttpStatusCode.BadRequest, "Missing subscription ID")
    }
}

/**
 * Route to subscribe for email notifications
 */
fun Route.subscribeEmailNotificationRoute() {
    post("/subscribe/email") {
        val emailSubscription = call.receive<EmailSubscription>()
        val result = SubscribeEmailNotifications().run(emailSubscription)
        call.respond(result)
    }
}

/**
 * Route to subscribe for webhook notifications
 */
fun Route.subscribeWebhookRoute() {
    post("/subscribe/webhook") {
        val webhookSubscription = call.receive<WebhookSubscription>()
        val result = SubscribeWebhookNotifications().run(webhookSubscription)
        call.respond(result)
    }
}

/**
 * Route to unsubscribe from a notification
 */
fun Route.unsubscribeNotificationRoute() {
    post("/unsubscribe") {
        val subscription = call.receive<UnsubscribeRequest>()
        val result = UnsubscribeNotifications().run(subscription.subscriptionId)
        call.respond(result)
    }
}

fun Route.healthCheckRoute() {
    get("/health") {
        val result = HealthQueryService().getHealth()
        val responseCode = when (result.status) {
            HealthStatusType.STATUS_UP -> HttpStatusCode.OK
            else -> HttpStatusCode.InternalServerError
        }
        call.respond(responseCode, result)
    }
}

fun Route.versionRoute() {
    val version = environment?.config?.propertyOrNull("ktor.version")?.getString() ?: "unknown"
    val gitProps = Properties()
    javaClass.getResourceAsStream("/git.properties")?.use {
        gitProps.load(it)
    }
    get("/version") {
        call.respond(mapOf(
            "version" to version,
            "branch" to gitProps["git.branch"],
            "commit" to gitProps["git.commit.id.abbrev"],
            "commitId" to gitProps["git.commit.id"],
            "commitTime" to gitProps["git.commit.time"]
        ))
    }
}