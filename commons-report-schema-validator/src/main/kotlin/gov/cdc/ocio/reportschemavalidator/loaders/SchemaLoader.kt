package gov.cdc.ocio.reportschemavalidator.loaders

import java.io.File

/**
 * The interface which loads the schema files from the class path
 */
interface SchemaLoader {
    fun loadSchemaFile(fileName: String): File?
}
