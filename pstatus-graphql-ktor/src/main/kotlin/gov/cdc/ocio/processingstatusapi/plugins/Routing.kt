package gov.cdc.ocio.processingstatusapi.plugins

import gov.cdc.ocio.processingstatusapi.queries.HealthQueryService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HealthQueryService().getHealth())
        }
    }
}