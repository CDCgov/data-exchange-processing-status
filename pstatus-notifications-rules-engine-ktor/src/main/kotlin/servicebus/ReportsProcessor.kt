package gov.cdc.ocio.processingstatusnotifications.servicebus

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext
import com.azure.messaging.servicebus.models.DeadLetterOptions
import gov.cdc.ocio.messagesystem.MessageSystemProcessor
import gov.cdc.ocio.messagesystem.plugins.createAzureServiceBusPlugin
import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import io.ktor.server.application.*
import org.slf4j.LoggerFactory

class ReportsProcessor : MessageSystemProcessor {
    private val logger = LoggerFactory.getLogger(ReportsNotificationProcessor::class.java)

    override fun processMessage(context: ServiceBusReceivedMessageContext) {
        val message = context.message

        logger.trace(
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
            logger.warn("Unable to parse the message: {}", e.localizedMessage)
            val deadLetterOptions = DeadLetterOptions()
                .setDeadLetterReason("Validation failed")
                .setDeadLetterErrorDescription(e.message)
            context.deadLetter(deadLetterOptions)
            logger.info("Message sent to the dead-letter queue.")
        } catch (e: Exception) {
            logger.warn("Failed to process service bus message: {}", e.localizedMessage)
        }
    }
}


/**
 * The main application module which runs always
 */
fun Application.serviceBusModule() {
    install(createAzureServiceBusPlugin(ReportsProcessor()))
}
