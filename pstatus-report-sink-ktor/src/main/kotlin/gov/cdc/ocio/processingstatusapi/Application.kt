package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.processingstatusapi.plugins.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

enum class MessageSystem {
    AWS,
    AZURE_SERVICE_BUS,
    RABBITMQ
}

/**
 * Load the environment configuration values
 *
 * @receiver KoinApplication
 * @param environment ApplicationEnvironment
 * @return KoinApplication
 */
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val databaseModule = DatabaseKoinCreator.moduleFromAppEnv(environment)

    val messageSystemModule = module {
        val msgType = environment.config.property("ktor.message_system").getString()
        single {msgType} // add msgType to Koin Modules

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
            MessageSystem.AWS.toString() -> {
                single(createdAtStart = true) {
                    AWSSQServiceConfiguration(environment.config, configurationPath = "aws")
                }
            }
        }
    }

    return modules(listOf(databaseModule, messageSystemModule))
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

        MessageSystem.AWS -> {
            awsSQSModule()
        }
        else -> log.error("Invalid message system configuration")
    }

    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
    }
}
