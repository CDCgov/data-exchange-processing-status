package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.processingstatusapi.messagesystems.*
import gov.cdc.ocio.processingstatusapi.models.MessageSystemType
import gov.cdc.ocio.processingstatusapi.plugins.*
import gov.cdc.ocio.reportschemavalidator.utils.SchemaLoaderKoinCreator
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import mu.KotlinLogging
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin


private fun createMessageSystem(environment: ApplicationEnvironment): MessageSystem {
    return when (getMessageSystem(environment)) {
        MessageSystemType.AZURE_SERVICE_BUS -> {
            val config = AzureServiceBusConfiguration(environment.config, configurationPath = "azure")
            AzureServiceBusMessageSystem(config)
        }
        MessageSystemType.RABBITMQ -> {
            val config = RabbitMQServiceConfiguration(environment.config, configurationPath = "rabbitMQ")
            RabbitMQMessageSystem(config.getConnectionFactory().newConnection())
        }
        MessageSystemType.AWS -> {
            val config = AWSSQSServiceConfiguration(environment.config, configurationPath = "aws")
            AWSSQSMessageSystem(config.createSQSClient(), config.queueURL)
        }
        else -> { UnsupportedMessageSystem(environment.config.property("ktor.message_system").getString()) }
    }
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
    val schemaLoaderModule = SchemaLoaderKoinCreator.getSchemaLoaderFromAppEnv(environment)
    val messageSystemModule = module {
        single(createdAtStart = true) { createMessageSystem(environment) }
    }

    return modules(
        listOf(
            databaseModule,
            messageSystemModule,
            schemaLoaderModule
        )
    )
}

/**
 * The main function
 *  @param args Array<string>
 */
fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

private fun getMessageSystem(environment: ApplicationEnvironment): MessageSystemType? {
    val logger = KotlinLogging.logger {}

    // Determine which messaging system module to load
    val currentMessagingSystem = environment.config.property("ktor.message_system").getString()
    val messageSystemType: MessageSystemType? = try {
        MessageSystemType.valueOf(currentMessagingSystem.uppercase())
    } catch (e: IllegalArgumentException) {
        logger.error("Unrecognized message system: $currentMessagingSystem")
        null
    }
    return messageSystemType
}

/**
 * The main application module which always runs and loads other modules
 */
fun Application.module() {
    configureRouting()

    when (getMessageSystem(environment)) {
        MessageSystemType.AZURE_SERVICE_BUS -> {
            serviceBusModule()
        }
        MessageSystemType.RABBITMQ -> {
            rabbitMQModule()
        }
        MessageSystemType.AWS -> {
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
