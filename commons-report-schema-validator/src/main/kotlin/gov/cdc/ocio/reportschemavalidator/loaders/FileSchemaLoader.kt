package gov.cdc.ocio.reportschemavalidator.loaders

import java.io.File

/**
 * The class which loads the schema files from the class path
 */
class FileSchemaLoader : SchemaLoader {

    /**
     * The function which loads the schema based on the file name path and returns a File or null
     * @param fileName String
     * @return File?
     */
    override fun loadSchemaFile(fileName: String): File? {
        val schemaDirectoryPath = "schema"
        val schemaFilePath = javaClass.classLoader.getResource("$schemaDirectoryPath/$fileName")
        return schemaFilePath?.let { File(it.toURI()) }?.takeIf { it.exists() }
    }
}
