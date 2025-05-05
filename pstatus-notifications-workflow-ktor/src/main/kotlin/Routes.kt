@file:Suppress("PLUGIN_IS_NOT_ENABLED")
package gov.cdc.ocio.processingnotifications

import gov.cdc.ocio.processingnotifications.model.*
import gov.cdc.ocio.processingnotifications.service.*
import gov.cdc.ocio.types.model.WorkflowSubscriptionDeadlineCheck
import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
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
        val subscription = call.receive<WorkflowSubscriptionDeadlineCheck>()
        val result = DeadLineCheckSubscriptionService().run(subscription)
        call.respond(result)
    }
}

/**
 * Route to subscribe for upload digest counts
 */
fun Route.subscribeUploadDigestCountsRoute() {
    post("/subscribe/uploadDigestCounts") {
        val subscription = call.receive<WorkflowSubscriptionForDataStreams>()
        val result = UploadDigestCountsNotificationSubscriptionService()
            .run(subscription)
        call.respond(result)
    }
}

/**
 * Route to subscribe for top data stream errors notification subscription
 */
fun Route.subscribeDataStreamTopErrorsNotification() {
    post("/subscribe/dataStreamTopErrorsNotification") {
        val subscription = call.receive<WorkflowSubscriptionForDataStreams>()
        val result = DataStreamTopErrorsNotificationSubscriptionService()
            .run(subscription)
        call.respond(result)
    }
}

/**
 * Route to unsubscribe from a workflow notification subscription
 */
fun Route.unsubscribe() {
    post("/unsubscribe") {
        val unsubRequest = call.receive<UnsubscribeRequest>()
        val result = NotificationSubscriptionService().unsubscribe(unsubRequest.subscriptionId)
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