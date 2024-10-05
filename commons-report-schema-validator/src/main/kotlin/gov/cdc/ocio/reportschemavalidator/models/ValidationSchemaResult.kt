package gov.cdc.ocio.reportschemavalidator.gov.cdc.ocio.reportschemavalidator.models

data class ValidationSchemaResult(val reason:String , val status: Boolean, val invalidData: MutableList<String>)
