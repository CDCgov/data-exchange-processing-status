package gov.cdc.ocio.reportschemavalidator.validators

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult
import java.io.File

/**
 * The interface to validate JSON data against schemas.
 */

interface SchemaValidator {
    fun validateSchema(schemaFileName: String, jsonNode: JsonNode, schemaFile: File, objectMapper: ObjectMapper,
                       invalidData: MutableList<String>): ValidationSchemaResult
}
