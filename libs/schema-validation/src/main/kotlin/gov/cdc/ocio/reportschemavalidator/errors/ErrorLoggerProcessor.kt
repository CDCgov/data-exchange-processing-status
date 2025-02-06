package gov.cdc.ocio.reportschemavalidator.errors

import gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult
import mu.KotlinLogging


/**
 * The class for processing and handling of errors
 */
class ErrorLoggerProcessor : ErrorProcessor {

    private val logger = KotlinLogging.logger {}

    /**
     * Function that processes the error and returns the validation result
     * @param reason String
     * @param invalidData MutableList<String>
     * @return ValidationSchemaResult
     */
    override fun processError(
        reason: String,
        schemaFileNames: MutableList<String>,
        invalidData: MutableList<String>
    ): ValidationSchemaResult {

        logger.error(reason)
        invalidData.add(reason)
        return ValidationSchemaResult(reason, false, schemaFileNames, invalidData)
    }

}
