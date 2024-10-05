package org.example.gov.cdc.ocio.reportschemavalidator.errors

import gov.cdc.ocio.reportschemavalidator.gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult

interface ErrorProcessor {
    fun processError(reason: String, invalidData: MutableList<String>):ValidationSchemaResult
}
