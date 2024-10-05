package gov.cdc.ocio.reportschemavalidator.schema

import java.io.File
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import gov.cdc.ocio.reportschemavalidator.gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult

class JsonSchemaValidator : SchemaValidator {

     companion object {
         //Use the LONG_OR_DOUBLE number policy, which will prevent Longs from being made into Doubles
         val logger = KotlinLogging.logger {}

     }
       override fun validateSchema(
         schemaFileName: String,
         jsonNode: JsonNode,
         schemaFile: File,
         objectMapper: ObjectMapper,
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
           return ValidationSchemaResult(reason,status,invalidData)
     }


 }
