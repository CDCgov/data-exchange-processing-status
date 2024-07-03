package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.processingstatusapi.cosmos.CosmosConfiguration
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosDeadLetterRepository
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.plugins.AzureServiceBusConfiguration
import gov.cdc.ocio.processingstatusapi.plugins.configureRouting
import gov.cdc.ocio.processingstatusapi.plugins.serviceBusModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin


/**
 * Load the environment configuration values
 * Instantiate a singleton CosmosDatabase container instance
 * @param environment ApplicationEnvironment
 */
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val cosmosModule = module {
        val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
        val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
        single(createdAtStart = true) { CosmosRepository(uri, authKey, "Reports", "/uploadId") }
        single(createdAtStart = true) { CosmosDeadLetterRepository(uri, authKey, "Reports-DeadLetter", "/uploadId") }

        // Create an azure service bus config that can be dependency injected (for health checks)
        single(createdAtStart = true) { CosmosConfiguration(uri, authKey) }
    }
    val asbConfigModule = module {
        // Create an azure service bus config that can be dependency injected (for health checks)
        single(createdAtStart = true) { AzureServiceBusConfiguration(environment.config, configurationPath = "azure.service_bus") }
    }

    return modules(listOf(cosmosModule, asbConfigModule))
}

/**
 * The main function
 *  @param args Array<string>
 */
fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

/**
 * The main application module which always runs and loads other modules
 */
fun Application.module() {
    configureRouting()
    serviceBusModule()
    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
    }
}
