package gov.cdc.ocio.processingstatusapi.plugins

import gov.cdc.ocio.processingstatusapi.health.HealthQueryService
import gov.cdc.ocio.types.health.HealthStatusType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


/**
 * Configures the REST routing endpoints for the application.
 *
 * @receiver Application
 */
fun Application.configureRouting() {
    val version = environment.config.propertyOrNull("ktor.version")?.getString() ?: "unknown"
    routing {
        get("/health") {
            val result = HealthQueryService().getHealth()
            val responseCode = when (result.status) {
                HealthStatusType.STATUS_UP -> HttpStatusCode.OK
                else -> HttpStatusCode.InternalServerError
            }
            call.respond(responseCode, result)
        }
        get("/version") {
            call.respondText(version)
        }
    }
}
