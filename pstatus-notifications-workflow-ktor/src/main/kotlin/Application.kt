package gov.cdc.ocio.processingnotifications

import gov.cdc.ocio.processingnotifications.cosmos.CosmosConfiguration
import gov.cdc.ocio.processingnotifications.cosmos.CosmosRepository
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin


fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val cosmosModule = module {
        val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
        val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
        single(createdAtStart = true) { CosmosRepository(uri, authKey, "Reports", "/uploadId") }

        // Create a CosmosDB config that can be dependency injected (for health checks)
        single(createdAtStart = true) { CosmosConfiguration(uri, authKey) }
    }

    return modules(listOf(cosmosModule))
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
        healthCheckRoute()
    }

}
