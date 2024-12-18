package gov.cdc.ocio.reportschemavalidator.loaders

import gov.cdc.ocio.reportschemavalidator.models.SchemaFile


/**
 * The interface which loads the schema files from the class path
 */
interface SchemaLoader {
    fun loadSchemaFile(fileName: String): SchemaFile
}
