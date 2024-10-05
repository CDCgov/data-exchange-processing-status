package gov.cdc.ocio.reportschemavalidator.schema

import java.io.File

interface SchemaLoader {
    fun loadSchemaFile(fileName: String): File?
}
