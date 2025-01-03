package gov.cdc.ocio.processingstatusapi.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.expediagroup.graphql.dataloader.KotlinDataLoaderRegistryFactory
import com.expediagroup.graphql.server.ktor.*
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDataLoader
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDeadLetterDataLoader
import gov.cdc.ocio.processingstatusapi.mutations.*
import gov.cdc.ocio.processingstatusapi.queries.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import mu.KotlinLogging
import java.time.Duration
import java.util.*


/**
 * Implementation of the GraphQL module for PS API.
 *
 * @receiver Application
 */
fun Application.graphQLModule() {
    val logger = KotlinLogging.logger {}

    install(WebSockets) {
        // needed for subscriptions
        pingPeriod = Duration.ofSeconds(1)
        contentConverter = JacksonWebsocketContentConverter()
    }
    // see https://ktor.io/docs/server-jwt.html#configure-verifier
    // Get security settings and default to enabled if missing
    val securityEnabled = environment.config.tryGetString("jwt.enabled")?.lowercase() != "false"
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()
    val graphQLPath = environment.config.tryGetString("graphql.path")
    if (securityEnabled) {
        install(Authentication) {
            jwt {
                jwt("auth-jwt") {
                    realm = myRealm
                    verifier(
                        JWT
                            .require(Algorithm.HMAC256(Base64.getDecoder().decode(secret)))
                            .withAudience(audience)
                            .withIssuer(issuer)
                            .build()
                    )
                    validate { credential ->
                        if (credential.payload.getClaim("username").asString() != "") {
                            JWTPrincipal(credential.payload)
                        } else {
                            logger.error("username missing from JWT claims, denying the token")
                            null
                        }
                    }
                    challenge { defaultScheme, realm ->
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            "Token is not valid or has expired; defaultScheme = $defaultScheme, realm = $realm"
                        )
                    }
                }
            }
        }
    }
    install(StatusPages) {
        customGraphQLStatusPages()
    }
//    install(CORS) {
//        anyHost()
//    }

//    val reportMutation by inject<ReportMutation>() // Inject ReportMutation from Koin

    install(GraphQL) {
        schema {
            packages = listOf("gov.cdc.ocio.processingstatusapi")
            queries = listOf(
                HealthQueryService(),
                ReportQueryService(),
                ReportSchemaQueryService(),
                ReportCountsQueryService(),
                ReportDeadLetterQueryService(),
                UploadQueryService()

            )
            mutations= listOf(
                NotificationsMutationService(),
                DataStreamTopErrorsNotificationSubscriptionMutationService(),
                DeadlineCheckSubscriptionMutationService(),
                UploadErrorsNotificationSubscriptionMutationService(),
                UploadDigestCountsSubscriptionMutationService(),
                ReportMutation()

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
        if (securityEnabled) {
            server {
                contextFactory = CustomGraphQLContextFactory()
            }
        }
    }
    install(Routing) {
        if (securityEnabled) {
            authenticate("auth-jwt") {
                graphQLPostRoute()
            }
        } else {
            graphQLPostRoute()
        }
        graphQLSubscriptionsRoute()
        graphQLSDLRoute()
        // Go to http://localhost:8080/graphiql for the GraphQL playground
        if (graphQLPath.isNullOrEmpty()) {
            graphiQLRoute()
        } else {
            graphiQLRoute(
                graphQLEndpoint = "$graphQLPath/graphql",
                subscriptionsEndpoint = "$graphQLPath/subscriptions"
            )
        }
    }
}