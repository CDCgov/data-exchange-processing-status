package gov.cdc.ocio.processingstatusnotifications

import gov.cdc.ocio.messagesystem.configs.AzureServiceBusConfiguration
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin


/**
 * Load the environment configuration values
 * Instantiate a singleton CosmosDatabase container instance
 * @param environment ApplicationEnvironment
 */
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {

    val asbConfigModule = module {
        // Create an azure service bus config that can be dependency injected (for health checks)
        single(createdAtStart = true) { AzureServiceBusConfiguration(environment.config, configurationPath = "azure.service_bus") }
    }

    return modules(listOf(asbConfigModule))
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
        subscribeEmailNotificationRoute()
        unsubscribeEmailNotificationRoute()
        subscribeWebhookRoute()
        unsubscribeWebhookRoute()
        healthCheckRoute()
        versionRoute()
    }

}
