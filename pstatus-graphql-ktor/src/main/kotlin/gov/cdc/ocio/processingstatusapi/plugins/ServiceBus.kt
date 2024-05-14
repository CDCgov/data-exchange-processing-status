package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.messaging.servicebus.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.config.*
import java.util.concurrent.TimeUnit


class AzureServiceBusConfiguration(config: ApplicationConfig) {
    var connectionString: String = config.tryGetString("connection_string") ?: ""
    var queueName: String = config.tryGetString("queue_name") ?: ""
}

val AzureServiceBus = createApplicationPlugin(
    name = "AzureServiceBus",
    configurationPath = "azure.service_bus",
    createConfiguration = ::AzureServiceBusConfiguration) {

    val connectionString = pluginConfig.connectionString
    val queueName = pluginConfig.queueName

    val processorClient by lazy {
        ServiceBusClientBuilder()
            .connectionString(connectionString)
            .processor()
            .queueName(queueName)
            .processMessage{ context -> processMessage(context) }
            .processError { context -> processError(context) }
            .buildProcessorClient()
    }

    // handles received messages
    @Throws(InterruptedException::class)
    fun receiveMessages() {
        // Create an instance of the processor through the ServiceBusClientBuilder
        println("Starting the Azure service bus processor")
        println("connectionString = $connectionString, queueName = $queueName")
        processorClient.start()
    }

    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Server is started")
        receiveMessages()
    }
    on(MonitoringEvent(ApplicationStopped)) { application ->
        application.log.info("Server is stopped")
        println("Stopping and closing the processor")
        processorClient.close()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStarted) {}
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }
}

private fun processMessage(context: ServiceBusReceivedMessageContext) {
    val message = context.message
    System.out.printf(
        "Processing message. Session: %s, Sequence #: %s. Contents: %s%n", message.getMessageId(),
        message.sequenceNumber, message.getBody()
    )
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
