package gov.cdc.ocio.processingstatusapi.plugins

import gov.cdc.ocio.processingstatusapi.loaders.ForbiddenException
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun StatusPagesConfig.customGraphQLStatusPages(): StatusPagesConfig {
    exception<Throwable> { call, cause ->
        when (cause) {
            is UnsupportedOperationException -> call.respond(HttpStatusCode.MethodNotAllowed)
            is ForbiddenException -> call.respond(HttpStatusCode.Forbidden, cause.message ?: "Forbidden")
            else -> call.respond(HttpStatusCode.BadRequest)
        }
    }
    return this
}