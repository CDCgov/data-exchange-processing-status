package gov.cdc.ocio.reportschemavalidator.errors

import gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult

/**
 * The interface for processing and handling of errors
 */
interface ErrorProcessor {
    fun processError(reason: String, schemaFileNames: MutableList<String>,invalidData: MutableList<String>):ValidationSchemaResult
}
