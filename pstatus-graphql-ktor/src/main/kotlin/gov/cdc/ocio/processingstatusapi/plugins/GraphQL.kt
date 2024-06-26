package gov.cdc.ocio.processingstatusapi.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.expediagroup.graphql.dataloader.KotlinDataLoaderRegistryFactory
import com.expediagroup.graphql.server.ktor.*
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDataLoader
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDeadLetterDataLoader
import gov.cdc.ocio.processingstatusapi.queries.*
import gov.cdc.ocio.processingstatusapi.mutations.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.time.Duration

fun Application.graphQLModule() {
    install(WebSockets) {
        // needed for subscriptions
        pingPeriod = Duration.ofSeconds(1)
        contentConverter = JacksonWebsocketContentConverter()
    }
    // see https://ktor.io/docs/server-jwt.html#configure-verifier
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()
    val graphQLPath = environment.config.property("graphql.path").getString()
    install(Authentication) {
        jwt {
            jwt("auth-jwt") {
                realm = myRealm
                verifier(JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build())
                validate { credential ->
                    if (credential.payload.getClaim("username").asString() != "") {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
                challenge { _, _ ->
                    call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
                }
            }
        }
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
                ReportCountsQueryService(),
                ReportDeadLetterQueryService(),
                UploadQueryService()

            )
            mutations= listOf(
                NotificationsMutationService()
            )
//            subscriptions = listOf(
//                ErrorSubscriptionService()
//            )
            hooks = CustomSchemaGeneratorHooks()
        }
        engine {
            dataLoaderRegistryFactory = KotlinDataLoaderRegistryFactory(
                ReportDataLoader,
                ReportDeadLetterDataLoader
            )
        }
    }
    install(Routing) {
        authenticate("auth-jwt") {
//            post("/graphql") {
//                val principal = call.principal<JWTPrincipal>()
//                val username = principal!!.payload.getClaim("username").asString()
//                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
//                call.respondText("Hello, $username! Token is expired at $expiresAt ms.")
//            }
        }
        graphQLPostRoute()
        graphQLSubscriptionsRoute()
        graphQLSDLRoute()
        graphiQLRoute(
            graphQLEndpoint = "$graphQLPath/graphql",
            subscriptionsEndpoint = "$graphQLPath/subscriptions") // Go to http://localhost:8080/graphiql for the GraphQL playground
    }
}