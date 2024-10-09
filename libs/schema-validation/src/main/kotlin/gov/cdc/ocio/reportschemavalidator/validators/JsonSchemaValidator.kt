package gov.cdc.ocio.reportschemavalidator.validators

import java.io.File
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult
import mu.KLogger

/**
 * The class that validates the Json against the schemas using networknt library
 * @param logger KLogger
 */

class JsonSchemaValidator(private val logger: KLogger) : SchemaValidator {

    /**
     * The function which validates the schema file against the node passed in using the networknt library
     * @param schemaFileName String
     * @param jsonNode JsonNode
     * @param schemaFile File
     * @param objectMapper ObjectMapper
     * @param invalidData MutableList
     * @return ValidationSchemaResult
     */
    override fun validateSchema(
        schemaFileName: String,
        jsonNode: JsonNode,
        schemaFile: File,
        objectMapper: ObjectMapper,
        schemaFileNames: MutableList<String>,
        invalidData: MutableList<String>,
    ):ValidationSchemaResult {

        var status = false
        var  reason = "The report could not be validated against the JSON schema: $schemaFileName."
        logger.info("Schema file base: $schemaFileName")
        val schemaNode: JsonNode = objectMapper.readTree(schemaFile)
        val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
        val schema: JsonSchema = schemaFactory.getSchema(schemaNode)
        val schemaValidationMessages: Set<ValidationMessage> = schema.validate(jsonNode)

        if (schemaValidationMessages.isEmpty()) {
            reason="The report has been successfully validated against the JSON schema:$schemaFileName."
            logger.info(reason)
            status= true
        } else {
            schemaValidationMessages.forEach { invalidData.add(it.message) }
            //  processError(reason, invalidData,validationSchemaFileNames,createReportMessage)
        }
        return ValidationSchemaResult(reason,status,schemaFileNames,invalidData)
    }
}
