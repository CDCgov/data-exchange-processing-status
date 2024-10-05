package gov.cdc.ocio.reportschemavalidator.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.reportschemavalidator.gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult
import java.io.File


interface SchemaValidator {
    fun validateSchema(schemaFileName: String, jsonNode: JsonNode, schemaFile: File, objectMapper: ObjectMapper,
                               invalidData: MutableList<String>): ValidationSchemaResult

}
