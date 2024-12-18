package gov.cdc.ocio.reportschemavalidator.models

import java.io.InputStream

/**
 * Data class for defining a schema file.
 *
 * @property fileName String
 * @property inputStream InputStream?
 * @constructor
 */
data class SchemaFile(
    val fileName: String,
    val inputStream: InputStream?
) {
    val exists = inputStream != null
}
