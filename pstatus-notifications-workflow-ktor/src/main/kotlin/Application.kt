package gov.cdc.ocio.processingnotifications

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.notificationdispatchers.NotificationDispatcherKoinCreator
import gov.cdc.ocio.processingnotifications.config.TemporalConfig
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import io.grpc.StatusRuntimeException
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
import io.opentelemetry.semconv.ServiceAttributes
import io.temporal.client.WorkflowNotFoundException
import io.temporal.client.WorkflowServiceException
import org.koin.ktor.plugin.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.module


fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val databaseModule = DatabaseKoinCreator.moduleFromAppEnv(environment)
    val notificationDispatcherModule = NotificationDispatcherKoinCreator.moduleFromAppEnv(environment)
    val temporalModule = module {
        val serviceTarget = environment.config.tryGetString("temporal.service_target") ?: "localhost:7233"
        val namespace = environment.config.tryGetString("temporal.namespace") ?: "default"
        val temporalConfig = TemporalConfig(serviceTarget, namespace)
        single(createdAtStart = true) {
            WorkflowEngine(temporalConfig)
        }
    }
    return modules(listOf(databaseModule, notificationDispatcherModule, temporalModule))
}

fun main(args: Array<String>) {
    // Set the JVM to headless mode. The primary purpose of this is to prevent XChart from spinning up a Java GUI app
    // during any line of code that instantiates a chart. A chart never actually shows anything in the GUI app, but
    // one is spun up. Nothing will happen server-side, but when running locally, such as on a Mac this line prevents
    // the app from appearing. Note: XChart is what is used to display charts in the HTML based notification emails.
    System.setProperty("java.awt.headless", "true")

    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {
    val builder = AutoConfiguredOpenTelemetrySdk.builder().addResourceCustomizer { old, _ ->
        old.toBuilder()
            .putAll(old.attributes)
            .put(ServiceAttributes.SERVICE_NAME, environment.config.tryGetString("otel.service_name") ?: "pstatus-notifications-workflow")
            .build()
    }
    val otel: OpenTelemetry = builder.build().openTelemetrySdk
    GlobalOpenTelemetry.set(otel)
    install(KtorServerTelemetry) {
        setOpenTelemetry(otel)
    }

    install(Koin) {
        loadKoinModules(environment)
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            // Serialize OffsetDateTime as ISO-8601
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    install(StatusPages) {
        // Map exceptions to HTTP status codes that will be returned
        val exceptionToHttpStatusCode = mapOf<Class<out Throwable>, HttpStatusCode>(
            IllegalArgumentException::class.java to HttpStatusCode.BadRequest,
            StatusRuntimeException::class.java to HttpStatusCode.InternalServerError,
            WorkflowServiceException::class.java to HttpStatusCode.BadRequest,
            WorkflowNotFoundException::class.java to HttpStatusCode.NotFound,
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
    routing {
        subscribeDeadlineCheckRoute()
        subscribeDataStreamTopErrorsNotification()
        subscribeUploadDigestCountsRoute()
        unsubscribe()
        getWorkflowsRoute()
        healthCheckRoute()
        versionRoute()
    }
}

