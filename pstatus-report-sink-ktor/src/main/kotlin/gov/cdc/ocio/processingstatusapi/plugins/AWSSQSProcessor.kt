package gov.cdc.ocio.processingstatusapi.plugins

import com.google.gson.JsonSyntaxException

import java.util.*

import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.CreateReportMessage
import gov.cdc.ocio.processingstatusapi.models.Source
import gov.cdc.ocio.processingstatusapi.models.ValidationComponents
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService

/**
 * The AWS SQS service is an additional interface for receiving and validating reports.
 */
class AWSSQSProcessor {
    @Throws(BadRequestException::class)
    fun validateMessage(messageAsString: String) {

        val components = ValidationComponents.getComponents()

        try {
            components.logger.info { "Received message from AWS SQS: $messageAsString" }
            val message = SchemaValidation().checkAndReplaceDeprecatedFields(messageAsString)
            components.logger.info { "SQS message after checking for depreciated fields $message" }

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
                        components.gson.fromJson(message, CreateReportMessage::class.java),
                        Source.AWS
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
            components.logger.error("Failed to validate message received from AWS SQS: ${e.message}")
            throw e
        } catch (e: JsonSyntaxException) {
            components.logger.error("Failed to parse message received from AWS SQS: ${e.localizedMessage}")
            throw BadStateException("Unable to interpret the create report message")
        }
    }
}