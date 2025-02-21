package gov.cdc.ocio.reportschemavalidator.validators

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult


/**
 * The interface to validate JSON data against schemas.
 */

interface SchemaValidator {
    fun validateSchema(schemaFileName: String, jsonNode: JsonNode, schemaFile: SchemaFile, objectMapper: ObjectMapper,
                       schemaFileNames: MutableList<String>, invalidData: MutableList<String>): ValidationSchemaResult

    fun checkSchemaFile(schemaFileContent: String)
}
