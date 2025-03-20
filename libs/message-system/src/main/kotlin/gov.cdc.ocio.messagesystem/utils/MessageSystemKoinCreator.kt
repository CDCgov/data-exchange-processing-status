package gov.cdc.ocio.messagesystem.utils

import gov.cdc.ocio.messagesystem.MessageProcessorInterface
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.config.AWSSQSServiceConfiguration
import gov.cdc.ocio.messagesystem.config.AzureServiceBusConfiguration
import gov.cdc.ocio.messagesystem.config.RabbitMQServiceConfiguration
import gov.cdc.ocio.messagesystem.models.MessageSystemType
import gov.cdc.ocio.messagesystem.plugins.*
import gov.cdc.ocio.messagesystem.rabbitmq.RabbitMQMessageSystem
import gov.cdc.ocio.messagesystem.servicebus.AzureServiceBusMessageSystem
import gov.cdc.ocio.messagesystem.sqs.AWSSQSMessageSystem
import gov.cdc.ocio.messagesystem.unsupported.UnsupportedMessageSystem
import io.ktor.server.application.*
import io.ktor.server.config.*
import mu.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module


object MessageSystemKoinCreator {

    /**
     * Creates the message system if possible from the provided application environment variables.
     *
     * @param environment ApplicationEnvironment
     * @return MessageSystem
     */
    private fun createMessageSystem(
        environment: ApplicationEnvironment
    ): MessageSystem {
        val messageSystemType = MessageSystemType.getFromAppEnv(environment)
        return when (messageSystemType) {
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
            else -> {
                UnsupportedMessageSystem(environment.config.tryGetString("ktor.message_system"))
            }
        }
    }

    /**
     * Creates the message system module from the application environment.
     *
     * @param environment ApplicationEnvironment
     * @return Module
     */
    fun moduleFromAppEnv(environment: ApplicationEnvironment): Module {
        return module {
            single {
                createMessageSystem(environment)
            }
        }
    }
}

/**
 * Creates the message system ktor plugin from the message system type and message processor implementation provided.
 *
 * @receiver Application
 * @param messageSystemType MessageSystemType?
 * @param messageProcessorImpl MessageProcessorInterface
 */
fun Application.createMessageSystemPlugin(
    messageSystemType: MessageSystemType?,
    messageProcessorImpl: MessageProcessorInterface
) {
    val logger = KotlinLogging.logger {}

    when (messageSystemType) {
        MessageSystemType.AZURE_SERVICE_BUS -> {
            serviceBusModule(messageProcessorImpl)
        }
        MessageSystemType.RABBITMQ -> {
            rabbitMQModule(messageProcessorImpl)
        }
        MessageSystemType.AWS -> {
            awsSQSModule(messageProcessorImpl)
        }
        else -> {
            logger.error("Invalid message system configuration")
        }
    }
}
