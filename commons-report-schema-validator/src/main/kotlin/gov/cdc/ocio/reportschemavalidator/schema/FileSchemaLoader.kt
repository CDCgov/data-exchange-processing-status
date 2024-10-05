package gov.cdc.ocio.reportschemavalidator.schema

import java.io.File

class FileSchemaLoader : SchemaLoader {
    override fun loadSchemaFile(fileName: String): File? {
        val schemaDirectoryPath = "schema"
        val schemaFilePath = javaClass.classLoader.getResource("$schemaDirectoryPath/$fileName")
        return schemaFilePath?.let { File(it.toURI()) }?.takeIf { it.exists() }
    }
}
