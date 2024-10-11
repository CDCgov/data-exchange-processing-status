package gov.cdc.ocio.eventreadersink.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val version = environment.config.propertyOrNull("ktor.version")?.getString() ?: "unknown"
    routing {
        /*get("/health") {
            call.respond(HealthQueryService().getHealth())
        }*/
        get("/version") {
            call.respondText(version)
        }
    }
}