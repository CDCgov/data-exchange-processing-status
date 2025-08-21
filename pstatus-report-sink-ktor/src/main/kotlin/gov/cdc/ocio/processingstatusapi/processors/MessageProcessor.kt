package gov.cdc.ocio.processingstatusapi.processors

import com.google.gson.JsonSyntaxException
import gov.cdc.ocio.messagesystem.MessageProcessorInterface
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.exceptions.BadRequestException
import gov.cdc.ocio.messagesystem.exceptions.BadStateException
import gov.cdc.ocio.messagesystem.models.Source
import gov.cdc.ocio.messagesystem.models.ReportMessage
import gov.cdc.ocio.processingstatusapi.models.ValidationComponents
import gov.cdc.ocio.messagesystem.MessageProcessorConfig
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation
import gov.cdc.ocio.reportschemavalidator.loaders.SchemaLoader
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService
import io.opentelemetry.api.GlobalOpenTelemetry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


abstract class MessageProcessor: MessageProcessorInterface, KoinComponent {

    protected abstract val source: Source

    private val components = ValidationComponents.getComponents()

    private val schemaLoader by inject<SchemaLoader>()

    private val messageSystem by inject<MessageSystem>()

    private val messageProcessorConfig by inject<MessageProcessorConfig>()

    private val meter = GlobalOpenTelemetry.get().getMeter("report-sink")

    private val validReportCount = meter.counterBuilder("valid_report_count")
        .setDescription("Count of valid reports")
        .build()

    private val invalidReportCount = meter.counterBuilder("invalid_report_count")
        .setDescription("Count of invalid reports")
        .build()

    @Throws(BadRequestException::class, BadStateException::class)
    override fun processMessage(message: String) {
        try {
            components.logger.info { "Received message from $source : $message" }

            val isReportValidJson = components.jsonUtils.isJsonValid(message)

            if (!isReportValidJson) {
                components.logger.error { "Message is not in correct JSON format." }
                SchemaValidation().sendToDeadLetter("Validation failed. The message is not in JSON format.")
                return
            }
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
                    components.gson.fromJson(message, ReportMessage::class.java),
                    source
                )
                // Increment the otel valid report count
                validReportCount.add(1)

                // Forward the validated report if enabled
                if (messageProcessorConfig.forwardValidatedReports)
                    messageSystem.send(message)
            } else {
                components.logger.info { "The message failed to validate, creating dead-letter report." }
                SchemaValidation().sendToDeadLetter(
                    source,
                    validationResult.invalidData,
                    validationResult.schemaFileNames,
                    components.gson.fromJson(message, ReportMessage::class.java)
                )
                // Increment the otel invalid report count
                invalidReportCount.add(1)
                return
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
