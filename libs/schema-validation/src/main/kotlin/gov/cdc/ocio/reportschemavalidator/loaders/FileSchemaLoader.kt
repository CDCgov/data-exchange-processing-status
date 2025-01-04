package gov.cdc.ocio.reportschemavalidator.loaders

import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import java.io.FileNotFoundException


/**
 * The class which loads the schema files from the class path
 */
class FileSchemaLoader(private val config: Map<String, String>) : SchemaLoader {

    /**
     * The function which loads the schema based on the file name path and returns a [SchemaFile]
     * @param fileName String
     * @return [SchemaFile]
     */
    override fun loadSchemaFile(fileName: String): SchemaFile {
        val schemaLocalSystemFilePath = config["REPORT_SCHEMA_LOCAL_FILE_SYSTEM_PATH"]
            ?: throw IllegalArgumentException("Local file system path is not configured")
        val file = java.io.File("$schemaLocalSystemFilePath/$fileName")
        if (!file.exists()) {
            throw FileNotFoundException("Report rejected: file - ${fileName} not found for content schema.")
        }
        return SchemaFile(
            fileName = fileName,
            inputStream = file.inputStream()
        )
    }

}
