package gov.cdc.ocio.reportschemavalidator.loaders

import gov.cdc.ocio.reportschemavalidator.models.SchemaFile


/**
 * The class which loads the schema files from the class path
 */
class FileSchemaLoader : SchemaLoader {

    /**
     * The function which loads the schema based on the file name path and returns a [SchemaFile]
     * @param fileName String
     * @return [SchemaFile]
     */
    override fun loadSchemaFile(fileName: String): SchemaFile {
        val schemaDirectoryPath = "schema"
        return SchemaFile(
            fileName = fileName,
            inputStream = javaClass.classLoader.getResourceAsStream("$schemaDirectoryPath/$fileName")
        )
    }

}
