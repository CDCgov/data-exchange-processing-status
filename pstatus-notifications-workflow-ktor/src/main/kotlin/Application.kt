package gov.cdc.ocio.processingnotifications


import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.processingnotifications.config.TemporalConfig
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val databaseModule = DatabaseKoinCreator.moduleFromAppEnv(environment)
    val temporalConfigModule = module {
        single {
            val appConfig: ApplicationConfig = get()
            TemporalConfig(temporalServiceTarget =
            appConfig.property("temporal.temporal_service_target").getString())
        }
    }
    return modules(listOf(databaseModule, temporalConfigModule, module {single{environment.config}}))
}


fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {
    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
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
        healthCheckRoute()
    }

}

