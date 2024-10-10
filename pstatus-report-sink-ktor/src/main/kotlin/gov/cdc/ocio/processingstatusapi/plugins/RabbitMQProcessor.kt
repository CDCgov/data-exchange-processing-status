package gov.cdc.ocio.processingstatusapi.plugins

import com.google.gson.JsonSyntaxException
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.CreateReportMessage
import gov.cdc.ocio.processingstatusapi.models.Source
import gov.cdc.ocio.processingstatusapi.utils.*
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation.Companion.gson

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
    fun validateMessage(messageAsString: String){
        try {
            SchemaValidation.logger.info { "Received message from RabbitMQ: $messageAsString" }
            val message = SchemaValidation().checkAndReplaceDeprecatedFields(messageAsString)
            SchemaValidation.logger.info { "RabbitMQ message after checking for depreciated fields $message" }

            /**
             * If validation is disabled and message is not a valid json, sends it to DLQ.
             * Otherwise, proceeds with schema validation.
             */
            val isValidationDisabled = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false
            val isReportValidJson = SchemaValidation().isJsonValid(message)

            if (isValidationDisabled) {
                if (!isReportValidJson) {
                    SchemaValidation.logger.error { "Message is not in correct JSON format." }
                    SchemaValidation().sendToDeadLetter("Validation failed.The message is not in JSON format.")
                    return
                }
            }else{
                if (isReportValidJson){
                    SchemaValidation.logger.info { "The message is in the correct JSON format. Proceed with schema validation" }
                    SchemaValidation().validateJsonSchema(message, Source.RABBITMQ)
                }else{
                    SchemaValidation.logger.error { "Validation is enabled, but the message is not in correct JSON format." }
                    SchemaValidation().sendToDeadLetter("The message is not in correct JSON format.")
                    return
                }
            }
            SchemaValidation.logger.info { "The message is valid creating report."}
            SchemaValidation().createReport(gson.fromJson(message, CreateReportMessage::class.java), Source.RABBITMQ)
        } catch (e: BadRequestException) {
            SchemaValidation.logger.error(e) { "Failed to validate rabbitMQ message ${e.message}" }
        }catch(e: JsonSyntaxException){
            SchemaValidation.logger.error(e) { "Failed to parse rabbitMQ message ${e.message}" }
        }
    }
}