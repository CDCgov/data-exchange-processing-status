package gov.cdc.ocio.reportschemavalidator.exceptions

/**
 * Schema validation exception
 *
 * @property issues Collection<String>
 * @property schemaFileNames Collection<String>
 * @constructor
 */
data class ValidationException(
    val issues: Collection<String>,
    val schemaFileNames: Collection<String>
) : Exception("Validation failed")