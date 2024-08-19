package gov.cdc.ocio.processingstatusapi.plugins

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.utils.checkAndReplaceDeprecatedFields
import mu.KotlinLogging
import gov.cdc.ocio.processingstatusapi.utils.isJsonValid

class RabbitMQProcessor {
    private val logger = KotlinLogging.logger {}


    private val gson = GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create()

    @Throws(BadRequestException::class)
    fun validateMessage(messageAsString: String){
        try {
            logger.debug { "Message received as original $messageAsString" }
            val message = checkAndReplaceDeprecatedFields(messageAsString)
            logger.debug { "Message after checking for depreciated fields $messageAsString" }

            /**
             * If validation is disabled and message is not a valid json, sends it to DLQ.
             * Otherwise, proceeds with schema validation.
             */
            val isValidationDisabled = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false
            if (isValidationDisabled) {
                if (!isJsonValid(message)){
                    logger.error { "Message is not in correct JSON format." }
                    //sendToDeadLetter("Validation failed.The message is not in JSON format.")
                }
            }else{
                println ("create function to validation schema")
            }
            println ("create report for valid message")
        } catch (e: BadRequestException) {
            logger.error(e) { "Failed to validate rabbitMQ message ${e.message}" }
        }catch(e: JsonSyntaxException){
            logger.error(e) { "Failed to parse rabbitMQ message ${e.message}" }
        }
    }
}