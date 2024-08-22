package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.messaging.servicebus.ServiceBusReceivedMessage
import com.google.gson.JsonSyntaxException
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.reports.CreateReportSBMessage
import gov.cdc.ocio.processingstatusapi.utils.*
import gov.cdc.ocio.processingstatusapi.utils.Helpers.gson
import gov.cdc.ocio.processingstatusapi.utils.Helpers.logger
import java.util.*

/**
 * The service bus is additional interface for receiving and validating reports.
 */
class ServiceBusProcessor {

    /**
     * Process a service bus message received from service bus queue or topic.
     *
     * @param message String
     * @throws BadRequestException
     * @throws JsonSyntaxException
     */
    @Throws(BadRequestException::class)
    fun withMessage(message: ServiceBusReceivedMessage) {
        var sbMessage = String(message.body.toBytes())

        try {
            logger.info { "Received message from Service Bus: $sbMessage" }
            sbMessage = checkAndReplaceDeprecatedFields(sbMessage)

            logger.info { "Service Bus message after checking for depreciated fields$sbMessage" }
            val disableValidation = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false

            if (disableValidation) {
                val isValid = isJsonValid(sbMessage)
                if (!isValid)
                    logger.error { "Message is not in correct JSON format." }
                    sendToDeadLetter("Validation failed.  The message is not in JSON format.")
                    return
            } else
                validateJsonSchema(sbMessage)
            logger.info { "The message is valid creating report."}
            createReport(gson.fromJson(sbMessage, CreateReportSBMessage::class.java))
        } catch (e: BadRequestException) {
            logger.error("Failed to validate service bus message ${e.message}")
            throw e
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse service bus message: ${e.localizedMessage}")
            throw BadStateException("Unable to interpret the create report message")
        }
    }
}