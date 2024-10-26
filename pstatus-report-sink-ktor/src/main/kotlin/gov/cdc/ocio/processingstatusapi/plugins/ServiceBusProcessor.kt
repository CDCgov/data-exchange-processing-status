package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.messaging.servicebus.ServiceBusReceivedMessage
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.JsonSyntaxException
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.CreateReportMessage
import gov.cdc.ocio.processingstatusapi.models.Source
import gov.cdc.ocio.processingstatusapi.utils.*
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation.Companion.gson
import gov.cdc.ocio.reportschemavalidator.errors.ErrorLoggerProcessor
import gov.cdc.ocio.reportschemavalidator.loaders.FileSchemaLoader
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.reportschemavalidator.validators.JsonSchemaValidator
import mu.KLogger
import mu.KotlinLogging
import java.util.*


/**
 * The service bus is additional interface for receiving and validating reports.
 */
class ServiceBusProcessor {
    private lateinit var jsonUtils: DefaultJsonUtils
    private lateinit var schemaValidationService: SchemaValidationService
    private lateinit var schemaLoader: FileSchemaLoader
    private lateinit var schemaValidator: JsonSchemaValidator
    private lateinit var errorProcessor: ErrorLoggerProcessor
    private lateinit var klogger: KLogger
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
        val objectMapper = ObjectMapper()
        jsonUtils = DefaultJsonUtils(objectMapper)
        schemaLoader = FileSchemaLoader()
        klogger = KotlinLogging.logger {}
        schemaValidator = JsonSchemaValidator(klogger)
        errorProcessor = ErrorLoggerProcessor(klogger)

        try {
            klogger.info { "Received message from Service Bus: $sbMessage" }
            sbMessage = SchemaValidation().checkAndReplaceDeprecatedFields(sbMessage)

            klogger.info { "Service Bus message after checking for depreciated fields$sbMessage" }
            val disableValidation = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false

            if (disableValidation) {
                val isValid = jsonUtils.isJsonValid(sbMessage)
                if (!isValid)
                    klogger.error { "Message is not in correct JSON format." }
                    SchemaValidation().sendToDeadLetter("Validation failed.  The message is not in JSON format.")
                    return
            } else
                schemaValidationService =
                    SchemaValidationService(schemaLoader, schemaValidator, errorProcessor, jsonUtils, klogger)

            klogger.info { "The message is in the correct JSON format. Proceed with schema validation" }
            val validationResult = schemaValidationService.validateJsonSchema(sbMessage)
            if (validationResult.status) {
                klogger.info { "The message has been successfully validated, creating report." }
                SchemaValidation().createReport(gson.fromJson(sbMessage, CreateReportMessage::class.java), Source.SERVICEBUS)
            } else {
                klogger.info { "The message failed to validate, creating dead-letter report." }
                SchemaValidation().sendToDeadLetter(
                    validationResult.invalidData,
                    validationResult.schemaFileNames,
                    gson.fromJson(sbMessage, CreateReportMessage::class.java)
                )
                return
            }
        } catch (e: BadRequestException) {
            klogger.error("Failed to validate service bus message ${e.message}")
            throw e
        } catch (e: JsonSyntaxException) {
            klogger.error("Failed to parse service bus message: ${e.localizedMessage}")
            throw BadStateException("Unable to interpret the create report message")
        }
    }
}