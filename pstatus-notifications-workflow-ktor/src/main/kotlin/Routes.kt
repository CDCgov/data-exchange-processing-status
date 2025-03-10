@file:Suppress("PLUGIN_IS_NOT_ENABLED")
package gov.cdc.ocio.processingnotifications

import gov.cdc.ocio.processingnotifications.model.*
import gov.cdc.ocio.processingnotifications.service.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*


/**
 * Route to subscribe for DeadlineCheck subscription
 */
fun Route.subscribeDeadlineCheckRoute() {
    post("/subscribe/deadlineCheck") {
        val subscription = call.receive<DeadlineCheckSubscription>()
        val deadlineCheckSubscription = DeadlineCheckSubscription(
            subscription.dataStreamId,
            subscription.dataStreamRoute,
            subscription.jurisdiction,
            subscription.cronSchedule,
            subscription.deliveryReference
        )
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
 * Route to subscribe for upload digest counts
 */
fun Route.subscribeUploadDigestCountsRoute() {
    post("/subscribe/uploadDigestCounts") {
        val subscription = call.receive<UploadDigestSubscription>()
        val uploadDigestCountsSubscription = UploadDigestSubscription(
            subscription.jurisdictionIds,
            subscription.dataStreamIds,
            subscription.cronSchedule,
            subscription.deliveryReference
        )
        val result = UploadDigestCountsNotificationSubscriptionService()
            .run(uploadDigestCountsSubscription)
        call.respond(result)
    }
}

/**
 * Route to unsubscribe for upload digest counts
 */
fun Route.unsubscribeUploadDigestCountsRoute() {
    post("/unsubscribe/uploadDigestCounts") {
        val subscription = call.receive<UploadDigestUnSubscription>()
        val result = UploadDigestCountNotificationUnSubscriptionService()
            .run(subscription.subscriptionId)
        call.respond(result)
    }
}

/**
 * Route to subscribe for upload errors notification subscription
 */
fun Route.subscribeUploadErrorsNotification() {
    post("/subscribe/uploadErrorsNotification") {
        val subscription = call.receive<UploadErrorsNotificationSubscription>()
        val uploadErrorsNotificationSubscription = UploadErrorsNotificationSubscription(
            subscription.dataStreamId,
            subscription.dataStreamRoute,
            subscription.jurisdiction,
            subscription.cronSchedule,
            subscription.deliveryReference
        )
        val result = UploadErrorsNotificationSubscriptionService()
            .run(uploadErrorsNotificationSubscription)
        call.respond(result)
    }
}

/**
 * Route to unsubscribe for upload errors subscription notification
 */
fun Route.unsubscribeUploadErrorsNotification() {
    post("/unsubscribe/uploadErrorsNotification") {
        val subscription = call.receive<UploadErrorsNotificationUnSubscription>()
        val result = UploadErrorsNotificationUnSubscriptionService()
            .run(subscription.subscriptionId)
        call.respond(result)
    }
}

/**
 * Route to subscribe for top data stream errors notification subscription
 */
fun Route.subscribeDataStreamTopErrorsNotification() {
    post("/subscribe/dataStreamTopErrorsNotification") {
        val subscription = call.receive<DataStreamTopErrorsNotificationSubscription>()
        val dataStreamTopErrorsNotificationSubscription = DataStreamTopErrorsNotificationSubscription(
            subscription.dataStreamId,
            subscription.dataStreamRoute,
            subscription.jurisdiction,
            subscription.cronSchedule,
            subscription.deliveryReference
        )
        val result = DataStreamTopErrorsNotificationSubscriptionService()
            .run(dataStreamTopErrorsNotificationSubscription)
        call.respond(result)
    }
}

/**
 * Route to unsubscribe for top data stream errors notification subscription
 */
fun Route.unsubscribesDataStreamTopErrorsNotification() {
    post("/unsubscribe/dataStreamTopErrorsNotification") {
        val subscription = call.receive<DataStreamTopErrorsNotificationUnSubscription>()
        val result = DataStreamTopErrorsNotificationUnSubscriptionService()
            .run(subscription.subscriptionId)
        call.respond(result)
    }
}

fun Route.getWorkflowsRoute() {
    get("/workflows") {
        val result = runCatching {
            val result = WorkflowStatusService().getAllWorkflows()
            call.respond(result)
        }
        result.onFailure {
            call.respond(
                HttpStatusCode.InternalServerError,
                result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
            )
        }
    }
}

/**
 * Route to subscribe for Temporal Server health check
 */
fun Route.healthCheckRoute() {
    get("/health") {
        call.respond(HealthCheckService().getHealth())
    }
}

/**
 * Route to get the version and git information of this service.
 */
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