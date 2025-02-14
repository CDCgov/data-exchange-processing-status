package gov.cdc.ocio.processingstatusnotifications.servicebus

import com.azure.core.amqp.AmqpTransportType
import com.azure.core.amqp.exception.AmqpException
import com.azure.messaging.servicebus.*
import com.azure.messaging.servicebus.models.DeadLetterOptions
import gov.cdc.ocio.messagesystem.configs.AzureServiceBusConfiguration
import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.config.*
import io.ktor.util.logging.*
import org.apache.qpid.proton.engine.TransportException
import java.util.concurrent.TimeUnit

internal val LOGGER = KtorSimpleLogger("pstatus-notifications")

val AzureServiceBus = createApplicationPlugin(
    name = "AzureServiceBus",
    configurationPath = "azure.service_bus",
    createConfiguration = ::AzureServiceBusConfiguration) {

    val connectionString = pluginConfig.connectionString
    val queueName = pluginConfig.queueName
    val topicName = pluginConfig.topicName
    val subscriptionName = pluginConfig.subscriptionName

    // Initialize Service Bus client for queue
    val processorQueueClient by lazy {
        ServiceBusClientBuilder()
            .connectionString(connectionString)
            .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
            .processor()
            .queueName(queueName)
            .processMessage{ context -> processMessage(context) }
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
            .processMessage{ context -> processMessage(context) }
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
            println("Starting the Azure service bus processor")
            println("connectionString = $connectionString, queueName = $queueName, topicName= $topicName, subscriptionName=$subscriptionName")
            processorQueueClient.start()
            processorTopicClient.start()
        }

        catch (e:AmqpException){
            println("Non-ServiceBusException occurred : ${e.message}")
        }
        catch (e:TransportException){
            println("Non-ServiceBusException occurred : ${e.message}")
        }

        catch (e:Exception){
            println("Non-ServiceBusException occurred : ${e.message}")
        }

    }

    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Server is started")
        receiveMessages()
    }
    on(MonitoringEvent(ApplicationStopped)) { application ->
        application.log.info("Server is stopped")
        println("Stopping and closing the processor")
        processorQueueClient.close()
        processorTopicClient.close()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStarted) {}
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }
}

/**
 * Function which processes the message received in the queue or topics
 *   @param context ServiceBusReceivedMessageContext
 *   @throws BadRequestException
 *   @throws IllegalArgumentException
 *   @throws Exception generic
 */
private fun processMessage(context: ServiceBusReceivedMessageContext) {
    val message = context.message

    LOGGER.trace(
        "Processing message. Session: {}, Sequence #: {}. Contents: {}",
        message.messageId,
        message.sequenceNumber,
        message.body
    )
    try {
        ReportsNotificationProcessor().withMessage(message)
    }
    //This will handle all missing required fields, invalid schema definition and malformed json all under the BadRequest exception and writes to dead-letter queue or topics depending on the context
    catch (e: BadRequestException) {
        LOGGER.warn("Unable to parse the message: {}", e.localizedMessage)
        val deadLetterOptions = DeadLetterOptions()
            .setDeadLetterReason("Validation failed")
            .setDeadLetterErrorDescription(e.message)
        context.deadLetter(deadLetterOptions)
        LOGGER.info("Message sent to the dead-letter queue.")
    }
    catch (e: Exception) {
        LOGGER.warn("Failed to process service bus message: {}", e.localizedMessage)
    }

}

/**
 * Function to handle and process the error generated during the processing of messages from queue or topics
 *  @param context ServiceBusErrorContext
 */
private fun processError(context: ServiceBusErrorContext) {
    System.out.printf(
        "Error when receiving messages from namespace: '%s'. Entity: '%s'%n",
        context.fullyQualifiedNamespace, context.entityPath
    )
    if (context.exception !is ServiceBusException) {
        System.out.printf("Non-ServiceBusException occurred: %s%n", context.exception)
        return
    }
    val exception = context.exception as ServiceBusException
    val reason = exception.reason
    if (reason === ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED || reason === ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND || reason === ServiceBusFailureReason.UNAUTHORIZED) {
        System.out.printf(
            "An unrecoverable error occurred. Stopping processing with reason %s: %s%n",
            reason, exception.message
        )
    } else if (reason === ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
        System.out.printf("Message lock lost for message: %s%n", context.exception)
    } else if (reason === ServiceBusFailureReason.SERVICE_BUSY) {
        try {
            // Choosing an arbitrary amount of time to wait until trying again.
            TimeUnit.SECONDS.sleep(1)
        } catch (e: InterruptedException) {
            System.err.println("Unable to sleep for period of time")
        }
    } else {
        System.out.printf(
            "Error source %s, reason %s, message: %s%n", context.errorSource,
            reason, context.exception
        )
    }
}

/**
 * The main application module which runs always
 */
fun Application.serviceBusModule() {
    install(AzureServiceBus) {
        // any additional configuration goes here
    }
}
