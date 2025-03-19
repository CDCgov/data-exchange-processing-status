package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.config.AWSSQSServiceConfiguration
import gov.cdc.ocio.messagesystem.config.RabbitMQServiceConfiguration
import gov.cdc.ocio.messagesystem.models.MessageSystemType
import gov.cdc.ocio.messagesystem.config.AzureServiceBusConfiguration
import gov.cdc.ocio.messagesystem.rabbitmq.RabbitMQMessageSystem
import gov.cdc.ocio.messagesystem.servicebus.AzureServiceBusMessageSystem
import gov.cdc.ocio.messagesystem.sqs.AWSSQSMessageSystem
import gov.cdc.ocio.messagesystem.unsupported.UnsupportedMessageSystem
import gov.cdc.ocio.processingstatusapi.models.MessageProcessorConfig
import gov.cdc.ocio.processingstatusapi.plugins.*
import gov.cdc.ocio.reportschemavalidator.utils.SchemaLoaderKoinCreator
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import mu.KotlinLogging
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin


/**
 * Creates the message system if possible from the provided application environment variables.
 *
 * @param environment ApplicationEnvironment
 * @return MessageSystem
 */
private fun createMessageSystem(environment: ApplicationEnvironment): MessageSystem {
    return when (getMessageSystem(environment)) {
        MessageSystemType.AZURE_SERVICE_BUS -> {
            val config = AzureServiceBusConfiguration(environment.config, configurationPath = "azure.service_bus")
            AzureServiceBusMessageSystem(config)
        }
        MessageSystemType.RABBITMQ -> {
            val config = RabbitMQServiceConfiguration(environment.config, configurationPath = "rabbitMQ")
            RabbitMQMessageSystem(config)
        }
        MessageSystemType.AWS -> {
            val config = AWSSQSServiceConfiguration(environment.config, configurationPath = "aws")
            AWSSQSMessageSystem(config.createSQSClient(), config.listenQueueURL, config.sendQueueURL)
        }
        else -> { UnsupportedMessageSystem(environment.config.tryGetString("ktor.message_system")) }
    }
}

/**
 * Creates the message processor configuration from the provided application environment.
 *
 * @param environment ApplicationEnvironment
 * @return MessageProcessorConfig
 */
private fun createMessageProcessorConfig(environment: ApplicationEnvironment): MessageProcessorConfig {
    val forwardValidatedReports = environment.config
        .tryGetString("ktor.message_processor.forward_validated_reports")
        ?.toBooleanStrictOrNull() ?: false
    return MessageProcessorConfig(
        forwardValidatedReports = forwardValidatedReports
    )
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
    val schemaLoaderModule = SchemaLoaderKoinCreator.moduleFromAppEnv(environment)
    val messageSystemModule = module {
        single(createdAtStart = true) { createMessageSystem(environment) }
        single { createMessageProcessorConfig(environment) }
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

/**
 * Retrieves the message system from the app environment and translates that into a message system enum if possible.
 *
 * @param environment ApplicationEnvironment
 * @return MessageSystemType?
 */
private fun getMessageSystem(environment: ApplicationEnvironment): MessageSystemType? {
    val logger = KotlinLogging.logger {}

    // Determine which messaging system module to load
    val currentMessagingSystem = environment.config.tryGetString("ktor.message_system") ?: ""
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
    // Set the environment variable dynamically for Logback
    System.setProperty("ENVIRONMENT", environment.config.property("ktor.logback.environment").getString())

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
