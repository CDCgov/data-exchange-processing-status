package gov.cdc.ocio.processingstatusapi.plugins

import com.expediagroup.graphql.dataloader.KotlinDataLoaderRegistryFactory
import com.expediagroup.graphql.server.ktor.*
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDataLoader
import gov.cdc.ocio.processingstatusapi.queries.HealthQueryService
import gov.cdc.ocio.processingstatusapi.queries.ReportQueryService
import gov.cdc.ocio.processingstatusapi.queries.UploadQueryService
import gov.cdc.ocio.processingstatusapi.subscriptions.ErrorSubscriptionService
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.time.Duration

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

fun Application.graphQLModule() {
    install(WebSockets) {
        // needed for subscriptions
        pingPeriod = Duration.ofSeconds(1)
        contentConverter = JacksonWebsocketContentConverter()
    }
    install(StatusPages) {
        defaultGraphQLStatusPages()
    }
//    install(CORS) {
//        anyHost()
//    }
    install(GraphQL) {
        schema {
            packages = listOf("gov.cdc.ocio.processingstatusapi")
            queries = listOf(
                HealthQueryService(),
                ReportQueryService(),
                UploadQueryService()
            )
            subscriptions = listOf(
                ErrorSubscriptionService()
            )
        }
        engine {
            dataLoaderRegistryFactory = KotlinDataLoaderRegistryFactory(
                ReportDataLoader
            )
        }
    }
    install(Routing) {
        graphQLPostRoute()
        graphQLSubscriptionsRoute()
        graphQLSDLRoute()
        graphiQLRoute() // Go to http://localhost:8080/graphiql for the GraphQL playground
    }
}