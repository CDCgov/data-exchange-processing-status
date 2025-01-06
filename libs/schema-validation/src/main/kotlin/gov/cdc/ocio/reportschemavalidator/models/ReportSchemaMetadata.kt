package gov.cdc.ocio.reportschemavalidator.models

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream


/**
 * Defines the model for providing the report schema metadata.
 *
 * @property filename String
 * @property schemaName String
 * @property schemaVersion String
 * @property description String
 * @constructor
 */
data class ReportSchemaMetadata(
    val filename: String,
    val schemaName: String,
    val schemaVersion: String,
    val description: String
) {
    companion object {

        /**
         * Convenience function to convert the provided parameters into a [ReportSchemaMetadata] object.
         *
         * @param schemaFilename String
         * @param schemaFile InputStream
         * @return ReportSchemaMetadata
         */
        fun from(schemaFilename: String, schemaFile: InputStream): ReportSchemaMetadata {
            val schemaNode = ObjectMapper().readTree(schemaFile)
            val titleNode = schemaNode.get("title")
            val description = if (titleNode.isTextual) titleNode.textValue() else "unknown"
            val versionRegex = """^(.+?)\.(\d+\.\d+(\.\d+)*)\.schema\.json$""".toRegex()
            return when (val versionMatch = versionRegex.matchEntire(schemaFilename)) {
                null -> ReportSchemaMetadata(schemaFilename, "unknown", "unknown", description)
                else -> {
                    val (schemaName, schemaVersion) = versionMatch.destructured
                    ReportSchemaMetadata(schemaFilename, schemaName, schemaVersion, description)
                }
            }
        }
    }
}
