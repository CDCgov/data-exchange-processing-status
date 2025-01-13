package gov.cdc.ocio.reportschemavalidator.models


/**
 * Data class for defining a schema file.
 *
 * @property fileName String
 * @property content String?
 * @constructor
 */
data class SchemaFile(
    val fileName: String,
    val content: String?
) {
    val exists = content != null
}
