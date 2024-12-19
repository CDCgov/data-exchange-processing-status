package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.core.amqp.AmqpTransportType
import com.azure.core.amqp.exception.AmqpException
import com.azure.messaging.servicebus.*
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation.Companion.logger
import gov.cdc.ocio.reportschemavalidator.loaders.CloudSchemaLoader
import gov.cdc.ocio.reportschemavalidator.utils.CloudSchemaLoaderConfiguration
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.config.*
import io.ktor.util.logging.*
import org.apache.qpid.proton.engine.TransportException
import java.util.concurrent.TimeUnit


/**
 * Class which initializes configuration values
 * @param config ApplicationConfig
 *
 */
class AzureServiceBusConfiguration(config: ApplicationConfig, configurationPath: String? = null) {
    private val configPath = if (configurationPath != null) "$configurationPath." else ""
    val connectionString = config.tryGetString("${configPath}service_bus.connection_string") ?: ""
    val queueName = config.tryGetString("${configPath}service_bus.queue_name") ?: ""
    val topicName = config.tryGetString("${configPath}service_bus.topic_name") ?: ""
    val subscriptionName = config.tryGetString("${configPath}service_bus.subscription_name") ?: ""

}

val AzureServiceBus  = createApplicationPlugin(
    name = "AzureServiceBus",
    configurationPath = "azure",
    createConfiguration = ::AzureServiceBusConfiguration) {

    val connectionString = pluginConfig.connectionString
    val queueName = pluginConfig.queueName
    val topicName = pluginConfig.topicName
    val subscriptionName = pluginConfig.subscriptionName
    val environment: ApplicationEnvironment = this@createApplicationPlugin.application.environment
    val schemaLoader = CloudSchemaLoaderConfiguration(environment).createSchemaLoader() // Create the schema loader here
    // Initialize Service Bus client for queue
    val processorQueueClient by lazy {
        ServiceBusClientBuilder()
            .connectionString(connectionString)
            .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
            .processor()
            .queueName(queueName)
            .processMessage{ context -> processMessage(context,schemaLoader) }
            .processError { context -> processError(context) }
            .buildProcessorClient()
    }

    // Initialize Service Bus client for topics
    val processorTopicClient by lazy {
        ServiceBusClientBuilder()
            .connectionString(connectionString)
            .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
            .processor()
            .topicName(topicName)
            .subscriptionName(subscriptionName)
            .processMessage{ context -> processMessage(context,schemaLoader) }
            .processError { context -> processError(context) }
            .buildProcessorClient()
    }

    /**
     * Function which starts receiving messages from queues and topics
     *  @throws AmqpException
     *  @throws TransportException
     *  @throws Exception generic
     */
    @Throws(InterruptedException::class)
    fun receiveMessages() {
        try {
            // Create an instance of the processor through the ServiceBusClientBuilder
            logger.info("Starting the Azure service bus processor")
            logger.info("queueName = $queueName, topicName= $topicName, subscriptionName=$subscriptionName")
            processorQueueClient.start()
            processorTopicClient.start()
        }
        catch (e:AmqpException) {
            logger.info("AmqpException occurred : ${e.message}")
        }
        catch (e:TransportException) {
            logger.info("TransportException occurred : ${e.message}")
        }
        catch (e:Exception) {
            logger.info("Non-ServiceBus exception occurred : ${e.message}")
        }
    }

    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Application started successfully.")
        receiveMessages()
    }
    on(MonitoringEvent(ApplicationStopped)) { application ->
        application.log.info("Application stopped successfully.")
        cleanupResourcesAndUnsubscribe(processorQueueClient, processorTopicClient, application)
    }
}

/**
 * Function which processes the message received in the queue or topics
 *   @param context ServiceBusReceivedMessageContext
 *   @throws BadRequestException
 *   @throws Exception generic
 */
private fun processMessage(context: ServiceBusReceivedMessageContext, schemaLoader: CloudSchemaLoader) {
    val message = context.message
    logger.trace(
        "Processing message. Session: {}, Sequence #: {}. Contents: {}",
        message.messageId,
        message.sequenceNumber,
        message.body
    )
    try {

        val serviceBusProcessor = ServiceBusProcessor()
        val messageToString = String(message.body.toBytes())
        serviceBusProcessor.processMessage(messageToString, schemaLoader)
    }
    catch (e: BadRequestException) {
        logger.warn("Unable to parse the message: {}", e.localizedMessage)
    }
    catch (e: Exception) {
        logger.warn("Failed to process service bus message: {}", e.localizedMessage)
    }
}

/**
 * Function to handle and process the error generated during the processing of messages from queue or topics
 *  @param context ServiceBusErrorContext
 */
private fun processError(context: ServiceBusErrorContext) {
    logger.error("Error when receiving messages from namespace: '${context.fullyQualifiedNamespace}', entity: '${context.entityPath}'")
    if (context.exception !is ServiceBusException) {
        logger.error("Non-ServiceBusException occurred: ${context.exception}")
        return
    }
    val exception = context.exception as ServiceBusException
    val reason = exception.reason
    if (reason === ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED || reason === ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND || reason === ServiceBusFailureReason.UNAUTHORIZED) {
        logger.error("An unrecoverable error occurred. Stopping processing with reason $reason: ${exception.message}")
    } else if (reason === ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
        logger.error("Message lock lost for message: ${context.exception}")
    } else if (reason === ServiceBusFailureReason.SERVICE_BUSY) {
        try {
            // Choosing an arbitrary amount of time to wait until trying again.
            TimeUnit.SECONDS.sleep(1)
        } catch (e: InterruptedException) {
            logger.error("Unable to sleep for period of time")
        }
    } else {
        logger.error("Error source ${context.errorSource}, reason $reason, message: ${context.exception}")
    }
}
/**
 * We need to clean up the resources and unsubscribe from application life events.
 * @param processorQueueClient The service bus `Queue` used for communicating with the queue.
 * @param processorTopicClient The service bus `Topic` used for communicating with the topic.
 * @param application The Ktor instance , provides access to the environment monitor used
 * for unsubscribing from events.
 */
private fun cleanupResourcesAndUnsubscribe(processorQueueClient:  ServiceBusProcessorClient,
                                           processorTopicClient: ServiceBusProcessorClient,
                                           application: Application) {
    application.log.info("Closing Service Bus Queue and Topic clients")
    processorQueueClient.close()
    processorTopicClient.close()
    application.environment.monitor.unsubscribe(ApplicationStarted){}
    application.environment.monitor.unsubscribe(ApplicationStopped){}
}
/**
 * The main application module which runs always
 */
fun Application.serviceBusModule() {
    install(AzureServiceBus)
}
