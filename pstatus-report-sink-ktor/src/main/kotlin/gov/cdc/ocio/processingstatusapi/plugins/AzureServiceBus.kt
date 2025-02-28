package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext
import gov.cdc.ocio.messagesystem.MessageSystemProcessor
import gov.cdc.ocio.messagesystem.exceptions.BadRequestException
import gov.cdc.ocio.messagesystem.plugins.createAzureServiceBusPlugin
import io.ktor.server.application.*
import mu.KotlinLogging

/**
 * The class which processes the message from service bus topic/subscription
 * It inherits the interface MessageSystemProcessor and overrides the processMesssage
 * function for processing the messages
 */
class ReportSinkProcessor : MessageSystemProcessor {

    override fun processMessage(context: ServiceBusReceivedMessageContext) {
        // Handle message specific to report-sink
        val message = context.message
        println("Report-Sink processing message: $message")

        val logger = KotlinLogging.logger {}

        logger.trace(
            "Processing message. Session: {}, Sequence #: {}. Contents: {}",
            message.messageId,
            message.sequenceNumber,
            message.body
        )
        try {
            val serviceBusProcessor = ServiceBusProcessor()
            val messageToString = String(message.body.toBytes())
            serviceBusProcessor.processMessage(messageToString)
        }
        catch (e: BadRequestException) {
            logger.warn("Unable to parse the message: {}", e.localizedMessage)
        }
        catch (e: Exception) {
            logger.warn("Failed to process service bus message: {}", e.localizedMessage)
        }
    }
}

/**
 * The main application module which runs always
 */
fun Application.serviceBusModule() {
    install(createAzureServiceBusPlugin(ReportSinkProcessor()))
}
