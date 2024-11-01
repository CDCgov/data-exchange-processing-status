package gov.cdc.ocio.processingstatusapi.plugins

import com.google.gson.JsonSyntaxException
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.CreateReportMessage
import gov.cdc.ocio.processingstatusapi.models.Source
import gov.cdc.ocio.processingstatusapi.models.ValidationComponents
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService

abstract class MessageProcessor {
    protected abstract val source: Source
    private val components = ValidationComponents.getComponents()

    @Throws(BadRequestException::class,BadStateException::class)
    fun processMessage(message: String) {
        try {
            components.logger.info { "Received message from $source : $message" }
            val updatedMessage = SchemaValidation().checkAndReplaceDeprecatedFields(message)
            components.logger.info { "Updated message if depreciated fields were found $message" }

            /**
             * If validation is disabled and message is not a valid json, sends it to DLQ.
             * Otherwise, proceeds with schema validation.
             */
            val isValidationDisabled = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false

            val isReportValidJson = components.jsonUtils.isJsonValid(updatedMessage)

            if (isValidationDisabled) {
                if (!isReportValidJson) {
                    components.logger.error { "Message is not in correct JSON format." }
                    SchemaValidation().sendToDeadLetter("Validation failed.The message is not in JSON format.")
                    return
                }
            } else {
                val schemaValidationService = SchemaValidationService(
                    components.schemaLoader,
                    components.schemaValidator,
                    components.errorProcessor,
                    components.jsonUtils,
                    components.logger
                )

                components.logger.info { "The message is in the correct JSON format. Proceed with schema validation" }
                val validationResult = schemaValidationService.validateJsonSchema(updatedMessage)

                if (validationResult.status) {
                    components.logger.info { "The message has been successfully validated, creating report." }
                    SchemaValidation().createReport(
                        components.gson.fromJson(message, CreateReportMessage::class.java),
                        source
                    )
                } else {
                    components.logger.info { "The message failed to validate, creating dead-letter report." }
                    SchemaValidation().sendToDeadLetter(
                        validationResult.invalidData,
                        validationResult.schemaFileNames,
                        components.gson.fromJson(message, CreateReportMessage::class.java)
                    )
                    return
                }
            }
        } catch (e: BadRequestException) {
            components.logger.error("Failed to validate message received from $source: ${e.message}")
            throw e
        } catch (e: JsonSyntaxException) {
            components.logger.error("Failed to parse message received from $source: ${e.localizedMessage}")
            throw BadStateException("Unable to interpret the create report message")
        }
    }
}

