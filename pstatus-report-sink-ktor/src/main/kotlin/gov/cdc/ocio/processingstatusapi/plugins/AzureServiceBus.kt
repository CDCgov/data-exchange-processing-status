package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.core.amqp.AmqpTransportType
import com.azure.core.amqp.exception.AmqpException
import com.azure.messaging.servicebus.*
import gov.cdc.ocio.messagesystem.config.AzureServiceBusConfiguration
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import mu.KotlinLogging
import org.apache.qpid.proton.engine.TransportException
import java.util.concurrent.TimeUnit


val azureServiceBusPlugin = createApplicationPlugin(
    name = "AzureServiceBus",
    configurationPath = "azure.service_bus",
    createConfiguration = ::AzureServiceBusConfiguration
) {

    val logger = KotlinLogging.logger {}

    val processor = ServiceBusProcessor()

    val queueName = pluginConfig.listenQueueName
    val topicName = pluginConfig.listenTopicName
    val subscriptionName = pluginConfig.subscriptionName

    val processorQueueClient by lazy {
        ServiceBusClientBuilder()
            .connectionString(pluginConfig.connectionString)
            .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
            .processor()
            .queueName(queueName)
            .processMessage { context -> processMessage(context, processor) }
            .processError { context -> processError(context) }
            .buildProcessorClient()
    }

    val processorTopicClient by lazy {
        ServiceBusClientBuilder()
            .connectionString(pluginConfig.connectionString)
            .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
            .processor()
            .topicName(topicName)
            .subscriptionName(subscriptionName)
            .processMessage { context -> processMessage(context, processor) }
            .processError { context -> processError(context) }
            .buildProcessorClient()
    }

    fun receiveMessages() {
        try {
            logger.info("Starting the Azure Service Bus processor")
            logger.info("queueName = $queueName, topicName= $topicName, subscriptionName=$subscriptionName")
            processorQueueClient.start()
            processorTopicClient.start()
        } catch (e: AmqpException) {
            logger.info("AmqpException occurred: ${e.message}")
        } catch (e: TransportException) {
            logger.info("TransportException occurred: ${e.message}")
        } catch (e: Exception) {
            logger.info("Non-ServiceBus exception occurred: ${e.message}")
        }
    }

    on(MonitoringEvent(ApplicationStarted)) {
        logger.info("Application started successfully.")
        receiveMessages()
    }

    on(MonitoringEvent(ApplicationStopped)) {
        logger.info("Application stopped successfully.")
        processorQueueClient.close()
        processorTopicClient.close()
    }
}

private fun processMessage(context: ServiceBusReceivedMessageContext, processor: ServiceBusProcessor) {
    val logger = KotlinLogging.logger {}

    val message = context.message
    logger.trace("Processing message. ID: {}, Sequence #: {}, Contents: {}",
        message.messageId, message.sequenceNumber, message.body
    )

    try {
        processor.processMessage(context.message.body.toString())
    } catch (e: Exception) {
        logger.warn("Failed to process message: {}", e.localizedMessage)
    }
}

private fun processError(context: ServiceBusErrorContext) {
    val logger = KotlinLogging.logger {}

    logger.error("Error when receiving messages from namespace: '${context.fullyQualifiedNamespace}', entity: '${context.entityPath}'")

    if (context.exception !is ServiceBusException) {
        logger.error("Non-ServiceBusException occurred: ${context.exception}")
        return
    }

    val exception = context.exception as ServiceBusException
    when (exception.reason) {
        ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED,
        ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND,
        ServiceBusFailureReason.UNAUTHORIZED -> {
            logger.error("Unrecoverable error occurred. Stopping processing: ${exception.message}")
        }
        ServiceBusFailureReason.MESSAGE_LOCK_LOST -> {
            logger.error("Message lock lost: ${context.exception}")
        }
        ServiceBusFailureReason.SERVICE_BUSY -> {
            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                logger.error("Unable to sleep before retry")
            }
        }
        else -> {
            logger.error("Error source ${context.errorSource}, reason ${exception.reason}, message: ${context.exception}")
        }
    }
}

fun Application.serviceBusModule() {
    install(azureServiceBusPlugin)
}