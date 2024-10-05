package gov.cdc.ocio.gov.cdc.ocio.reportschemavalidator.errors

import gov.cdc.ocio.reportschemavalidator.gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult
import mu.KotlinLogging
import org.example.gov.cdc.ocio.reportschemavalidator.errors.ErrorProcessor

class ErrorLoggerProcessor : ErrorProcessor {

    companion object {
         val logger = KotlinLogging.logger {}
   }
    override fun processError(reason: String, invalidData: MutableList<String>):ValidationSchemaResult {
        logger.error(reason)
        invalidData.add(reason)
       return ValidationSchemaResult(reason,false,invalidData)
    }

}
