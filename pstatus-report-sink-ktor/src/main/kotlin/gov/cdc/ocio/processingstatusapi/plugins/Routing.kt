package gov.cdc.ocio.processingstatusapi.plugins

import gov.cdc.ocio.processingstatusapi.HealthQueryService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configures the REST routing endpoints for the application.
 *
 * @receiver Application
 */
fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HealthQueryService().getHealth())
        }
    }
}
