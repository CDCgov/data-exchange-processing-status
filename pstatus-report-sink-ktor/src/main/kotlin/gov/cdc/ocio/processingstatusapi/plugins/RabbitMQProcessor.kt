package gov.cdc.ocio.processingstatusapi.plugins

import com.google.gson.JsonSyntaxException

import java.util.*

import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.CreateReportMessage
import gov.cdc.ocio.processingstatusapi.models.Source
import gov.cdc.ocio.processingstatusapi.models.ValidationComponents
import gov.cdc.ocio.processingstatusapi.utils.*
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService

/**
 * The RabbitMQ service is additional interface for receiving and validating reports for local runs.
 */
class RabbitMQProcessor {
    /**
     * Process a message received from rabbitMQ queue running locally.
     *
     * @param messageAsString String
     * @throws BadRequestException
     * @throws JsonSyntaxException
     */
    @Throws(BadRequestException::class)
    fun validateMessage(messageAsString: String) {

        val components = ValidationComponents.getComponents()

        try {
            components.logger.info { "Received message from RabbitMQ: $messageAsString" }
            val message = SchemaValidation().checkAndReplaceDeprecatedFields(messageAsString)
            components.logger.info  { "RabbitMQ message after checking for depreciated fields $message" }

            /**
             * If validation is disabled and message is not a valid json, sends it to DLQ.
             * Otherwise, proceeds with schema validation.
             */
            val isValidationDisabled = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false
            val isReportValidJson = components.jsonUtils.isJsonValid(message)

            if (isValidationDisabled) {
                if (!isReportValidJson) {
                    components.logger.error { "Message is not in correct JSON format." }
                    SchemaValidation().sendToDeadLetter("Validation failed.The message is not in JSON format.")
                    return
                }
            } else {
                components.logger.info { "The message is in the correct JSON format. Proceed with schema validation" }
                val schemaValidationService = SchemaValidationService(
                    components.schemaLoader,
                    components.schemaValidator,
                    components.errorProcessor,
                    components.jsonUtils,
                    components.logger
                )

                components.logger.info { "The message is in the correct JSON format. Proceed with schema validation" }
                val validationResult = schemaValidationService.validateJsonSchema(messageAsString)
                if (validationResult.status) {
                    components.logger.info { "The message has been successfully validated, creating report." }
                    SchemaValidation().createReport(
                        components.gson.fromJson(messageAsString, CreateReportMessage::class.java),
                        Source.RABBITMQ
                    )
                } else {
                    components.logger.info { "The message failed to validate, creating dead-letter report." }
                    SchemaValidation().sendToDeadLetter(
                        validationResult.invalidData,
                        validationResult.schemaFileNames,
                        components.gson.fromJson(messageAsString, CreateReportMessage::class.java)
                    )
                    return
                }
            }
        } catch (e: BadRequestException) {
            components.logger.error(e) { "Failed to validate rabbitMQ message ${e.message}" }
        } catch (e: JsonSyntaxException) {
            components.logger.error(e) { "Failed to parse rabbitMQ message ${e.message}" }
        }
    }
}