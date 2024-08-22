package gov.cdc.ocio.processingstatusapi.plugins

import com.google.gson.JsonSyntaxException
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.reports.CreateReportSBMessage
import gov.cdc.ocio.processingstatusapi.utils.*

/**
 * The RabbitMQ service is additional interface for receiving and validating reports for local runs.
 */
class RabbitMQProcessor {

    @Throws(BadRequestException::class)
    fun validateMessage(messageAsString: String){
        try {
            Helpers.logger.info { "Received message from RabbitMQ: $messageAsString" }
            val message = checkAndReplaceDeprecatedFields(messageAsString)
            Helpers.logger.info { "RabbitMQ message after checking for depreciated fields $messageAsString" }

            /**
             * If validation is disabled and message is not a valid json, sends it to DLQ.
             * Otherwise, proceeds with schema validation.
             */
            val isValidationDisabled = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false
            val isReportValidJson = isJsonValid(messageAsString)

            if (isValidationDisabled) {
                if (!isJsonValid(messageAsString)){
                    Helpers.logger.error { "Message is not in correct JSON format." }
                    sendToDeadLetter("Validation failed.The message is not in JSON format.")
                    return
                }
            }else{
                if (isReportValidJson){
                    Helpers.logger.info { "The message is in the correct JSON format. Proceed with schema validation" }
                    validateJsonSchema(message)
                }else{
                    Helpers.logger.error { "Validation is enabled, but the message is not in correct JSON format." }
                    sendToDeadLetter("The message is not in correct JSON format.")
                    return
                }
            }
            Helpers.logger.info { "The message is valid creating report."}
            createReport(Helpers.gson.fromJson(message, CreateReportSBMessage::class.java))
        } catch (e: BadRequestException) {
            Helpers.logger.error(e) { "Failed to validate rabbitMQ message ${e.message}" }
        }catch(e: JsonSyntaxException){
            Helpers.logger.error(e) { "Failed to parse rabbitMQ message ${e.message}" }
        }
    }
}