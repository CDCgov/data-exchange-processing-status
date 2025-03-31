package gov.cdc.ocio.processingstatusapi.processors

import com.google.gson.JsonSyntaxException
import gov.cdc.ocio.messagesystem.MessageProcessorInterface
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.exceptions.BadRequestException
import gov.cdc.ocio.messagesystem.exceptions.BadStateException
import gov.cdc.ocio.messagesystem.models.Source
import gov.cdc.ocio.messagesystem.models.CreateReportMessage
import gov.cdc.ocio.processingstatusapi.models.ValidationComponents
import gov.cdc.ocio.processingstatusapi.models.MessageProcessorConfig
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation
import gov.cdc.ocio.reportschemavalidator.loaders.SchemaLoader
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


abstract class MessageProcessor: MessageProcessorInterface, KoinComponent {

    protected abstract val source: Source

    private val components = ValidationComponents.getComponents()

    private val schemaLoader by inject<SchemaLoader>()

    private val messageSystem by inject<MessageSystem>()

    private val messageProcessorConfig by inject<MessageProcessorConfig>()

    @Throws(BadRequestException::class, BadStateException::class)
    override fun processMessage(message: String) {
        try {
            components.logger.info { "Received message from $source : $message" }

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
                    schemaLoader,
                    components.schemaValidator,
                    components.errorProcessor,
                    components.jsonUtils,
                    components.logger
                )

                components.logger.info { "The message is in the correct JSON format. Proceed with schema validation" }
                val validationResult = schemaValidationService.validateJsonSchema(message)

                if (validationResult.status) {
                    components.logger.info { "The message has been successfully validated, creating report." }
                    SchemaValidation().createReport(
                        components.gson.fromJson(message, CreateReportMessage::class.java),
                        source
                    )

                    // Forward the validated report if enabled
                    if (messageProcessorConfig.forwardValidatedReports)
                        messageSystem.send(message)
                } else {
                    components.logger.info { "The message failed to validate, creating dead-letter report." }
                    SchemaValidation().sendToDeadLetter(
                        source,
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

