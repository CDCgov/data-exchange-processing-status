package gov.cdc.ocio.processingstatusnotifications

import gov.cdc.ocio.database.telemetry.Otel
import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.messagesystem.models.MessageSystemType
import gov.cdc.ocio.messagesystem.utils.MessageSystemKoinCreator
import gov.cdc.ocio.messagesystem.utils.createMessageSystemPlugin
import gov.cdc.ocio.notificationdispatchers.NotificationDispatcherKoinCreator
import gov.cdc.ocio.processingstatusnotifications.processors.*
import gov.cdc.ocio.processingstatusnotifications.subscription.CachedSubscriptionLoader
import gov.cdc.ocio.processingstatusnotifications.subscription.DatabaseSubscriptionLoader
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.ktor.v2_0.KtorServerTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.sdk.metrics.InstrumentSelector
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.semconv.ServiceAttributes
import org.koin.core.KoinApplication
import org.koin.ktor.plugin.Koin
import org.koin.dsl.module


/**
 * Load the environment configuration values
 *
 * @param environment ApplicationEnvironment
 */
fun KoinApplication.loadKoinModules(
    environment: ApplicationEnvironment
): KoinApplication {
    val databaseModule = DatabaseKoinCreator.moduleFromAppEnv(environment)
    val messageSystemModule = MessageSystemKoinCreator.moduleFromAppEnv(environment)
    val subscriptionLoaderModule = module { single { CachedSubscriptionLoader(DatabaseSubscriptionLoader()) } }
    val notificationDispatcherModule = NotificationDispatcherKoinCreator.moduleFromAppEnv(environment)
    return modules(listOf(databaseModule, messageSystemModule, subscriptionLoaderModule, notificationDispatcherModule))
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {

    routing {
        notificationSubscriptionsRoute()
        notificationSubscriptionRoute()
        subscribeEmailNotificationRoute()
        subscribeWebhookRoute()
        unsubscribeNotificationRoute()
        healthCheckRoute()
        versionRoute()
    }

    createMessageSystemPlugin(MessageSystemType.getFromAppEnv(environment), MessageProcessor())

    val builder = AutoConfiguredOpenTelemetrySdk.builder()
        .setResultAsGlobal()
        .addResourceCustomizer { old, _ ->
        old.toBuilder()
            .putAll(old.attributes)
            .put(ServiceAttributes.SERVICE_NAME, environment.config.tryGetString("otel.service_name") ?: "pstatus-notifications-rules-engine")
            .build()
    }
        .addMeterProviderCustomizer { old, _ ->
            old.registerView(
                InstrumentSelector.builder().setType(InstrumentType.HISTOGRAM).build(), Otel.getDefaultHistogramView())
        }
    val otel: OpenTelemetry = builder.build().openTelemetrySdk
    install(KtorServerTelemetry) {
        setOpenTelemetry(otel)
    }

    install(Koin) {
        loadKoinModules(environment)
    }

    install(ContentNegotiation) {
        jackson()
    }

    install(StatusPages) {
        // Map exceptions to HTTP status codes that will be returned
        val exceptionToHttpStatusCode = mapOf<Class<out Throwable>, HttpStatusCode>(
            IllegalArgumentException::class.java to HttpStatusCode.BadRequest,
            IllegalStateException::class.java to HttpStatusCode.InternalServerError,
        )

        // Intercept all exceptions that occur during routing and return them as bad request errors with the error
        // message.
        exception<Exception> { call, cause ->
            // Check to see if there is an internal cause and provide that if so as those are typically more helpful
            // than the outer cause.
            val errorMessage = cause.cause?.message ?: cause.message
            val httpStatusCode = exceptionToHttpStatusCode[cause.javaClass] ?: HttpStatusCode.InternalServerError
            call.respond(httpStatusCode, mapOf("error" to errorMessage))
        }
    }
}
