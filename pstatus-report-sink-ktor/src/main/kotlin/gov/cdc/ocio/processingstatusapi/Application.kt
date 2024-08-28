package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.processingstatusapi.cosmos.CosmosConfiguration
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosDeadLetterRepository
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.plugins.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

enum class MessageSystem{
    AWS,
    AZURE_SERVICE_BUS,
    RABBITMQ
}

/**
 * Load the environment configuration values
 * Instantiate a singleton CosmosDatabase container instance
 * @param environment ApplicationEnvironment
 */
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val msgType = environment.config.property("ktor.message_system").getString()
    val cosmosModule = module {
        val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
        val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
        single(createdAtStart = true) { CosmosRepository(uri, authKey, "Reports", "/uploadId") }
        single(createdAtStart = true) { CosmosDeadLetterRepository(uri, authKey, "Reports-DeadLetter", "/uploadId") }

        //  Create a CosmosDB config that can be dependency injected (for health checks)
        single(createdAtStart = true) { CosmosConfiguration(uri, authKey) }
    }

    val configModule = module {
        when (msgType) {
            MessageSystem.AZURE_SERVICE_BUS.toString() -> {
                single(createdAtStart = true) {
                    AzureServiceBusConfiguration(environment.config, configurationPath = "azure.service_bus") }

            }
            MessageSystem.RABBITMQ.toString() -> {
                single(createdAtStart = true) {
                    RabbitMQServiceConfiguration(environment.config, configurationPath = "rabbitMQ")
                }

            }
        }
    }
   return modules(listOf(cosmosModule , configModule))
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

    //determine which messaging system module to load
    val messageSystem: MessageSystem? =  try {
        val currentMessagingSystem = environment.config.property("ktor.message_system").getString()
        MessageSystem.valueOf(currentMessagingSystem)
    } catch (e: IllegalArgumentException) {
        log.error("Invalid message system configuration: ${e.message}")
        null
    }

    when (messageSystem) {
        MessageSystem.AZURE_SERVICE_BUS -> {
            serviceBusModule()
        }
        MessageSystem.RABBITMQ -> {
            rabbitMQModule()
        }

        MessageSystem.AWS -> TODO()
        null -> TODO()
    }

    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
    }
}
