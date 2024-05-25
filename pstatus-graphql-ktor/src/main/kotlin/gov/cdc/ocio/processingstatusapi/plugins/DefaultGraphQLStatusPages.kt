package gov.cdc.ocio.processingstatusapi.plugins

import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

/**
 * Configures default exception handling using Ktor Status Pages.
 *
 * Returns following HTTP status codes:
 * * 405 (Method Not Allowed) - when attempting to execute mutation or query through a GET request
 * * 400 (Bad Request) - any other exception
 */
fun StatusPagesConfig.defaultGraphQLStatusPages(): StatusPagesConfig {
    exception<Throwable> { call, cause ->
        when (cause) {
            is UnsupportedOperationException -> call.respond(HttpStatusCode.MethodNotAllowed)
            else -> call.respond(HttpStatusCode.BadRequest)
        }
    }
    return this
}