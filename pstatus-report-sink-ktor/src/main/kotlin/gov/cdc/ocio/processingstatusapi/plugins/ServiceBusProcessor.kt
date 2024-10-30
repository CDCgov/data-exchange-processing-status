package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.messaging.servicebus.ServiceBusReceivedMessage

import com.google.gson.JsonSyntaxException

import java.util.*

import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.CreateReportMessage
import gov.cdc.ocio.processingstatusapi.models.Source
import gov.cdc.ocio.processingstatusapi.models.ValidationComponents
import gov.cdc.ocio.processingstatusapi.utils.*
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService


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

        val components = ValidationComponents.getComponents()

        try {
            components.logger.info { "Received message from Service Bus: $sbMessage" }
            sbMessage = SchemaValidation().checkAndReplaceDeprecatedFields(sbMessage)

            components.logger.info { "Service Bus message after checking for depreciated fields$sbMessage" }
            val disableValidation = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false

            if (disableValidation) {
                val isValid = components.jsonUtils.isJsonValid(sbMessage)
                if (!isValid)
                    components.logger.error { "Message is not in correct JSON format." }
                    SchemaValidation().sendToDeadLetter("Validation failed.  The message is not in JSON format.")
                    return
            } else {
                val schemaValidationService = SchemaValidationService(
                    components.schemaLoader,
                    components.schemaValidator,
                    components.errorProcessor,
                    components.jsonUtils,
                    components.logger
                )

                components.logger.info { "The message is in the correct JSON format. Proceed with schema validation" }
                val validationResult = schemaValidationService.validateJsonSchema(sbMessage)
                if (validationResult.status) {
                    components.logger.info { "The message has been successfully validated, creating report." }
                    SchemaValidation().createReport(
                        components.gson.fromJson(
                            sbMessage,
                            CreateReportMessage::class.java
                        ), Source.SERVICEBUS
                    )
                } else {
                    components.logger.info { "The message failed to validate, creating dead-letter report." }
                    SchemaValidation().sendToDeadLetter(
                        validationResult.invalidData,
                        validationResult.schemaFileNames,
                        components.gson.fromJson(sbMessage, CreateReportMessage::class.java)
                    )
                    return
                }
            }
        } catch (e: BadRequestException) {
            components.logger.error("Failed to validate service bus message ${e.message}")
            throw e
        } catch (e: JsonSyntaxException) {
            components.logger.error("Failed to parse service bus message: ${e.localizedMessage}")
            throw BadStateException("Unable to interpret the create report message")
        }
    }
}