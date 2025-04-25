package gov.cdc.ocio.processingstatusapi.plugins

import com.expediagroup.graphql.server.ktor.*
import gov.cdc.ocio.processingstatusapi.mutations.*
import gov.cdc.ocio.processingstatusapi.queries.*
import gov.cdc.ocio.processingstatusapi.security.configureSecurity
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.time.Duration
import kotlin.time.toKotlinDuration


/**
 * Implementation of the GraphQL module for PS API.
 *
 * @receiver Application
 */
fun Application.graphQLModule() {

    val securityEnabled = environment.config.tryGetString("security.enabled")?.lowercase() != "false"
    val graphQLPath = environment.config.tryGetString("graphql.path")
    val rulesEngineServiceUrl = environment.config.tryGetString("notifications.rules_engine_url")
    val workflowServiceUrl = environment.config.tryGetString("notifications.workflow_url")

    install(WebSockets) {
        // needed for subscriptions
        pingPeriod = Duration.ofSeconds(1).toKotlinDuration()
        contentConverter = JacksonWebsocketContentConverter()
    }

    configureSecurity(securityEnabled)

    install(GraphQL) {
        schema {
            packages = listOf(
                "gov.cdc.ocio.processingstatusapi",
                "gov.cdc.ocio.types" // for the types defined in the "commons-types" library
            )
            queries = listOf(
                HealthQueryService(),
                ReportQueryService(),
                ReportSchemaQueryService(),
                ReportCountsQueryService(),
                ReportDeadLetterQueryService(),
                UploadQueryService(),
                RulesEngineQueryService(rulesEngineServiceUrl),
                WorkflowQueryService(workflowServiceUrl)
            )
            mutations = listOf(
                NotificationsRulesEngineMutationService(rulesEngineServiceUrl),
                DataStreamTopErrorsNotificationSubscriptionMutationService(workflowServiceUrl),
                DeadlineCheckSubscriptionMutationService(workflowServiceUrl),
                UploadDigestCountsSubscriptionMutationService(workflowServiceUrl),
                ReportMutation(),
                ReportSchemaMutation()
            )
            hooks = CustomSchemaGeneratorHooks()
        }
        server {
            contextFactory = CustomGraphQLContextFactory()
        }
        engine {
            exceptionHandler = CustomGraphQLExceptionHandler()
        }
    }

    install(RoutingRoot) {
        if (securityEnabled) {
            authenticate("oauth") {
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