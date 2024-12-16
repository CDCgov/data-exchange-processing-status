package gov.cdc.ocio.reportschemavalidator.models

/**
 * The class which contains the result of the report schema validation
 * If there are issues then it would have a reason and the list of errors
 * If there are no errors then reason will have a success verbiage, status would be true and list will be empty
 * @param reason String
 * @param status Boolean
 * @param invalidData MutableList<String>
 */
data class ValidationSchemaResult(
    val reason: String,
    val status: Boolean,
    val schemaFileNames: MutableList<String>,
    val invalidData: MutableList<String>
)
