package gov.cdc.ocio.processingstatusapi.plugins

import com.google.gson.JsonSyntaxException
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.reports.CreateReportSBMessage
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation.Companion.gson
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation.Companion.logger

/**
 * The AWS SQS service is additional interface for receiving and validating reports.
 */
class AWSSQSProcessor {

    @Throws(BadRequestException::class)
    fun validateMessage(messageAsString: String){
        try {
            logger.info { "Received message from AWS SQS: $messageAsString" }

            val message = SchemaValidation().checkAndReplaceDeprecatedFields(messageAsString)
            logger.info { "SQS message after checking for depreciated fields $message" }
            /**
             * If validation is disabled and message is not a valid json, sends it to DLQ.
             * Otherwise, proceeds with schema validation.
             */
            val isValidationDisabled = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false
            val isReportValidJson = SchemaValidation().isJsonValid(message)

            if (isValidationDisabled) {
                if (!isReportValidJson) {
                    logger.error { "Message is not in correct JSON format." }
                    SchemaValidation().sendToDeadLetter("Validation failed.The message is not in JSON format.")
                    return
                }
            }else{
                if (isReportValidJson){
                    logger.info { "The message is in the correct JSON format. Proceed with schema validation" }
                    SchemaValidation().validateJsonSchema(message)
                }else{
                    logger.error { "Validation is enabled, but the message is not in correct JSON format." }
                    SchemaValidation().sendToDeadLetter("The message is not in correct JSON format.")
                    return
                }
            }
            logger.info { "The message is valid creating report."}
            SchemaValidation().createReport(gson.fromJson(message, CreateReportSBMessage::class.java))

        }catch (e: BadRequestException) {
            logger.error("Failed to validate message received from AWS SQS: ${e.message}")
            throw e
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse message received from AWS SQS: ${e.localizedMessage}")
            throw BadStateException("Unable to interpret the create report message")
        }
    }
}