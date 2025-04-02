package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.messagesystem.models.MessageSystemType
import gov.cdc.ocio.messagesystem.utils.MessageSystemKoinCreator
import gov.cdc.ocio.messagesystem.utils.createMessageSystemPlugin
import gov.cdc.ocio.messagesystem.MessageProcessorConfig
import gov.cdc.ocio.processingstatusapi.processors.AWSSQSProcessor
import gov.cdc.ocio.processingstatusapi.processors.RabbitMQProcessor
import gov.cdc.ocio.processingstatusapi.processors.ServiceBusProcessor
import gov.cdc.ocio.processingstatusapi.processors.UnsupportedProcessor
import gov.cdc.ocio.reportschemavalidator.utils.SchemaLoaderKoinCreator
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin


/**
 * Creates the message processor configuration from the provided application environment.
 *
 * @param environment ApplicationEnvironment
 * @return MessageProcessorConfig
 */
private fun createMessageProcessorConfig(
    environment: ApplicationEnvironment
): MessageProcessorConfig {
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
    val messageSystemModule = MessageSystemKoinCreator.moduleFromAppEnv(environment)
    val messageProcessorConfigModule = module { single { createMessageProcessorConfig(environment) } }

    return modules(
        listOf(
            databaseModule,
            schemaLoaderModule,
            messageSystemModule,
            messageProcessorConfigModule
        )
    )
}

/**
 * The main function
 * @param args Array<string>
 */
fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

/**
 * The main application module which always runs and loads other modules
 */
fun Application.module() {
    // Set the environment variable dynamically for Logback
    System.setProperty("ENVIRONMENT", environment.config.property("ktor.logback.environment").getString())

    configureRouting()

    val messageSystemType = MessageSystemType.getFromAppEnv(environment)

    val messageProcessor = when (messageSystemType) {
        MessageSystemType.AWS -> AWSSQSProcessor()
        MessageSystemType.AZURE_SERVICE_BUS -> ServiceBusProcessor()
        MessageSystemType.RABBITMQ -> RabbitMQProcessor()
        else -> UnsupportedProcessor()
    }

    createMessageSystemPlugin(messageSystemType, messageProcessor)

    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
    }
}
