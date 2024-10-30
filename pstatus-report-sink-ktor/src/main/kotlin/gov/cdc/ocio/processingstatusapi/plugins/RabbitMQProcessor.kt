package gov.cdc.ocio.processingstatusapi.plugins

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.database.utils.DateLongFormatTypeAdapter
import gov.cdc.ocio.database.utils.InstantTypeAdapter
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.CreateReportMessage
import gov.cdc.ocio.processingstatusapi.models.Source
import gov.cdc.ocio.processingstatusapi.utils.*
import gov.cdc.ocio.reportschemavalidator.errors.ErrorLoggerProcessor
import gov.cdc.ocio.reportschemavalidator.loaders.FileSchemaLoader
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.reportschemavalidator.validators.JsonSchemaValidator
import mu.KLogger
import mu.KotlinLogging
import java.time.Instant
import java.util.*

/**
 * The RabbitMQ service is additional interface for receiving and validating reports for local runs.
 */
class RabbitMQProcessor {
    private val objectMapper: ObjectMapper by lazy {ObjectMapper()}
    private val jsonUtils: DefaultJsonUtils by lazy {DefaultJsonUtils(objectMapper)}
    private lateinit var schemaValidationService: SchemaValidationService
    private val schemaLoader: FileSchemaLoader by lazy {FileSchemaLoader()}
    private val schemaValidator: JsonSchemaValidator by lazy {JsonSchemaValidator(SchemaValidation.logger)}
    private val errorProcessor: ErrorLoggerProcessor by lazy {ErrorLoggerProcessor(SchemaValidation.logger)}
    private val logger: KLogger by lazy {KotlinLogging.logger {}}

    private val gson: Gson by lazy {
        GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(Date::class.java, DateLongFormatTypeAdapter())
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
    }

    /**
     * Process a message received from rabbitMQ queue running locally.
     *
     * @param messageAsString String
     * @throws BadRequestException
     * @throws JsonSyntaxException
     */
    @Throws(BadRequestException::class)
    fun validateMessage(messageAsString: String) {

        try {
            logger.info { "Received message from RabbitMQ: $messageAsString" }
            val message = SchemaValidation().checkAndReplaceDeprecatedFields(messageAsString)
            logger.info { "RabbitMQ message after checking for depreciated fields $message" }

            /**
             * If validation is disabled and message is not a valid json, sends it to DLQ.
             * Otherwise, proceeds with schema validation.
             */
            val isValidationDisabled = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false
            val isReportValidJson = jsonUtils.isJsonValid(message)

            if (isValidationDisabled) {
                if (!isReportValidJson) {
                    logger.error { "Message is not in correct JSON format." }
                    SchemaValidation().sendToDeadLetter("Validation failed.The message is not in JSON format.")
                    return
                }
            } else {
                logger.info { "The message is in the correct JSON format. Proceed with schema validation" }
                schemaValidationService =
                    SchemaValidationService(schemaLoader, schemaValidator, errorProcessor, jsonUtils, logger)

                logger.info { "The message is in the correct JSON format. Proceed with schema validation" }
                val validationResult = schemaValidationService.validateJsonSchema(messageAsString)
                if (validationResult.status) {
                    logger.info { "The message has been successfully validated, creating report." }
                    SchemaValidation().createReport(
                        gson.fromJson(messageAsString, CreateReportMessage::class.java),
                        Source.RABBITMQ
                    )
                } else {
                    logger.info { "The message failed to validate, creating dead-letter report." }
                    SchemaValidation().sendToDeadLetter(
                        validationResult.invalidData,
                        validationResult.schemaFileNames,
                        gson.fromJson(messageAsString, CreateReportMessage::class.java)
                    )
                    return
                }
            }
        } catch (e: BadRequestException) {
            logger.error(e) { "Failed to validate rabbitMQ message ${e.message}" }
        } catch (e: JsonSyntaxException) {
            logger.error(e) { "Failed to parse rabbitMQ message ${e.message}" }
        }
    }
}