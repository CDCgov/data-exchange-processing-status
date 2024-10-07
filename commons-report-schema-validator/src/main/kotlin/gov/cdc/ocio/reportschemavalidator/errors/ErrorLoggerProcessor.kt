package gov.cdc.ocio.reportschemavalidator.errors

import gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult
import mu.KLogger

/**
 * The class for processing and handling of errors
 * @param logger KLogger
 */

class ErrorLoggerProcessor(private val logger:KLogger) : ErrorProcessor {

    /**
     * Function that processes the error and returns the validation result
     * @param reason String
     * @param invalidData MutableList<String>
     * @return ValidationSchemaResult
     */
    override fun processError(reason: String, invalidData: MutableList<String>):ValidationSchemaResult {
        logger.error(reason)
        invalidData.add(reason)
       return ValidationSchemaResult(reason,false,invalidData)
    }

}
