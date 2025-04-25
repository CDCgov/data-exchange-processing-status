package gov.cdc.ocio.reportschemavalidator.validators

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult
import mu.KotlinLogging


/**
 * JSON implementation of [SchemaValidator] that validates the Json against the schemas using networknt library.
 *
 * @property logger [KotlinLogging.logger]
 * @property specVersion [SpecVersion.VersionFlag]
 */
class JsonSchemaValidator : SchemaValidator {

    private val logger = KotlinLogging.logger {}

    private val specVersion = SpecVersion.VersionFlag.V7

    /**
     * The function which validates the schema file against the node passed in using the networknt library
     * @param schemaFileName String
     * @param jsonNode JsonNode
     * @param schemaFile SchemaFile
     * @param objectMapper ObjectMapper
     * @param invalidData MutableList
     * @return ValidationSchemaResult
     */
    override fun validateSchema(
        schemaFileName: String,
        jsonNode: JsonNode,
        schemaFile: SchemaFile,
        objectMapper: ObjectMapper,
        schemaFileNames: MutableList<String>,
        invalidData: MutableList<String>,
    ): ValidationSchemaResult {

        var status = false
        var reason = "The report could not be validated against the JSON schema: $schemaFileName."
        logger.info("Schema file base: $schemaFileName")
        val schemaNode = objectMapper.readTree(schemaFile.content)
        val schemaFactory = JsonSchemaFactory.getInstance(specVersion)
        val schema = schemaFactory.getSchema(schemaNode)
        val schemaValidationMessages = schema.validate(jsonNode)

        if (schemaValidationMessages.isEmpty()) {
            reason = "The report has been successfully validated against the JSON schema: $schemaFileName."
            logger.info(reason)
            status = true
        } else {
            schemaValidationMessages.forEach { invalidData.add(it.message) }
            //  processError(reason, invalidData,validationSchemaFileNames,reportMessage)
        }
        return ValidationSchemaResult(reason,status,schemaFileNames,invalidData)
    }

    /**
     * Provides basic validation on the schema file content provided, which should be JSON and the expected spec
     * version.
     *
     * @param schemaFileContent String
     */
    override fun checkSchemaFile(schemaFileContent: String) {
        val objectMapper = ObjectMapper()
        val schemaNode = objectMapper.readTree(schemaFileContent)
        val schemaFactory = JsonSchemaFactory.getInstance(specVersion)
        schemaFactory.getSchema(schemaNode)
    }
}
