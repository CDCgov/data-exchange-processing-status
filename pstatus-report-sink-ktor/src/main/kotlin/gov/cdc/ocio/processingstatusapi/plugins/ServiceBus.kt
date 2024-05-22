package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.core.amqp.AmqpTransportType
import com.azure.core.amqp.exception.AmqpException
import com.azure.core.exception.AzureException
import com.azure.messaging.servicebus.*
import com.azure.messaging.servicebus.models.DeadLetterOptions
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.config.*
import io.ktor.util.logging.*
import io.netty.channel.ConnectTimeoutException
import org.apache.qpid.proton.engine.TransportException
import java.util.concurrent.TimeUnit

internal val LOGGER = KtorSimpleLogger("pstatus-report-sink")

class AzureServiceBusConfiguration(config: ApplicationConfig) {
    var connectionString: String = config.tryGetString("connection_string") ?: ""
    var serviceBusNamespace: String = config.tryGetString("azure_servicebus_namespace") ?: ""
    var queueName: String = config.tryGetString("queue_name") ?: ""
    var topicName: String = config.tryGetString("topic_name") ?: ""
    var subscriptionName: String = config.tryGetString("subscription_name") ?: ""
}

val AzureServiceBus = createApplicationPlugin(
    name = "AzureServiceBus",
    configurationPath = "azure.service_bus",
    createConfiguration = ::AzureServiceBusConfiguration) {

    val connectionString = pluginConfig.connectionString
    var serviceBusNamespace= pluginConfig.serviceBusNamespace
    val queueName = pluginConfig.queueName
    val topicName = pluginConfig.topicName
    val subscriptionName = pluginConfig.subscriptionName

// Initialize Service Bus client for queue
    val processorQueueClient by lazy {
        ServiceBusClientBuilder()
            .connectionString(connectionString)
            .fullyQualifiedNamespace(serviceBusNamespace)
            .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
            .processor()
            .queueName(queueName)
            .processMessage{ context -> processMessage(context) }
            .processError { context -> processError(context) }
            .buildProcessorClient()
    }

    // Initialize Service Bus client for topic
    val processorTopicClient by lazy {
        ServiceBusClientBuilder()
            .connectionString(connectionString)
            .fullyQualifiedNamespace(serviceBusNamespace)
            .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
            .processor()
            .topicName(topicName)
            .subscriptionName(subscriptionName)
            .processMessage{ context -> processMessage(context) }
            .processError { context -> processError(context) }
            .buildProcessorClient()
    }

    // handles received messages
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

    fun sendMessage() {
       val senderClient =  ServiceBusClientBuilder()
           .connectionString(connectionString)
           .fullyQualifiedNamespace(serviceBusNamespace)
           .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
           .sender()
           .queueName(queueName)
           .buildClient()
       // val serviceBusMessages = reports.map {ServiceBusMessage(it)}
      //  senderClient.sendMessages(serviceBusMessages)
        try {
            val message = ServiceBusMessage("Hello, Service Bus!")
            senderClient.sendMessage(message)
            println("Message sent to the queue.")

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
        finally {
            senderClient.close()
        }
    }

    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Server is started")
        receiveMessages()
       // sendMessage() //****This is not working as well****
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


private fun processMessage(context: ServiceBusReceivedMessageContext) {
    val message = context.message

    LOGGER.trace(
        "Processing message. Session: {}, Sequence #: {}. Contents: {}",
        message.messageId,
        message.sequenceNumber,
        message.body
    )
    try {
        ServiceBusProcessor().withMessage(message.body.toString())
    } catch (e: BadRequestException) {
        LOGGER.warn("Unable to parse the message: {}", e.localizedMessage)
    }
    catch (e: IllegalArgumentException) { //  TODO : Is this the only exception at this time or more generic one???
        LOGGER.warn("Message rejected: {}", e.localizedMessage)
        //Writing to deadletter
        //  TODO : Will this do it for queue and topic based on the context.
        // TODO : Should this be "ValidationError" or something generic
         context.deadLetter(DeadLetterOptions().setDeadLetterReason("ValidationError").setDeadLetterErrorDescription(e.message))

         LOGGER.info("Message sent to the dead-letter queue.")
    }
    catch (e: Exception) {
        LOGGER.warn("Failed to process service bus message: {}", e.localizedMessage)
    }

}



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



fun Application.serviceBusModule() {
    install(AzureServiceBus) {
        // any additional configuration goes here
    }
}
