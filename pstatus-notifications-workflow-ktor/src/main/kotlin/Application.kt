package gov.cdc.ocio.processingnotifications

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.processingnotifications.config.TemporalConfig
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
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
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin


fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val databaseModule = DatabaseKoinCreator.moduleFromAppEnv(environment)
    val temporalModule = module {
        val serviceTarget = environment.config.tryGetString("temporal.service_target") ?: "localhost:7233"
        val namespace = environment.config.tryGetString("temporal.namespace") ?: "default"
        val temporalConfig = TemporalConfig(serviceTarget, namespace)
        single {
            WorkflowEngine(temporalConfig)
        }
    }
    return modules(listOf(databaseModule, temporalModule))
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {
    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())

            // Serialize OffsetDateTime as ISO-8601
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }
    }
    routing {
        subscribeDeadlineCheckRoute()
        unsubscribeDeadlineCheck()
        subscribeUploadErrorsNotification()
        unsubscribeUploadErrorsNotification()
        subscribeDataStreamTopErrorsNotification()
        unsubscribesDataStreamTopErrorsNotification()
        subscribeUploadDigestCountsRoute()
        unsubscribeUploadDigestCountsRoute()
        getWorkflowsRoute()
        healthCheckRoute()
        versionRoute()
    }

}

