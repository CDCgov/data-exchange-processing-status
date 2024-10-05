package gov.cdc.ocio.reportschemavalidator.gov.cdc.ocio.reportschemavalidator.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.ocio.reportschemavalidator.gov.cdc.ocio.reportschemavalidator.exceptions.BadRequestException
import gov.cdc.ocio.reportschemavalidator.gov.cdc.ocio.reportschemavalidator.exceptions.MalformedException
import gov.cdc.ocio.reportschemavalidator.gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult
import gov.cdc.ocio.reportschemavalidator.schema.SchemaLoader
import gov.cdc.ocio.reportschemavalidator.schema.SchemaValidator
import mu.KLogger
import org.example.gov.cdc.ocio.reportschemavalidator.errors.ErrorProcessor
import org.example.gov.cdc.ocio.reportschemavalidator.utils.JsonUtils
import java.io.File

class SchemaValidationService(
    private val schemaLoader: SchemaLoader,
    private val schemaValidator: SchemaValidator,
    private val errorProcessor: ErrorProcessor,
    private val jsonUtils: JsonUtils,
    private val logger: KLogger
) {

    fun validateJsonSchema(message: String, nodeText:String) : ValidationSchemaResult{
        val invalidData = mutableListOf<String>()
        val schemaFileNames = mutableListOf<String>()
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        var validationSchemaResult:ValidationSchemaResult

        try {

            val reportJsonNode = objectMapper.readTree(message)
            // validate and load base schema file
           val result  = loadAndVerifyBaseSchema(message,nodeText,invalidData)
           if(result.first == null && result.second!=null) return result.second!!
            val schemaFile = result.first!!
            schemaFileNames.add(schemaFile.name)
            // validate base schema
            validationSchemaResult = schemaValidator.validateSchema(schemaFile.name,reportJsonNode,schemaFile,objectMapper,invalidData)
            if(!validationSchemaResult.status) return  validationSchemaResult
            // validate content type
            validationSchemaResult= validateContentType(reportJsonNode,invalidData)
            if(!validationSchemaResult.status) return  validationSchemaResult
            // validate content
            validationSchemaResult= validateContent(reportJsonNode,invalidData)
            if(!validationSchemaResult.status) return  validationSchemaResult
            // validate content schema node and content schema version
            validationSchemaResult= validateContentSchemaNodeVersion(reportJsonNode,invalidData)
            if(!validationSchemaResult.status) return  validationSchemaResult
           //validate content file based on the content schema name and content schema node
            val contentValidationResult= validateContentSchemaFile(reportJsonNode,invalidData)
            if(!contentValidationResult.second.status) return  validationSchemaResult
            val contentSchemaFile = contentValidationResult.first
            val contentSchemaFileName =contentSchemaFile!!.name
            schemaFileNames.add(contentSchemaFileName)
            //validate content schema
            validationSchemaResult = schemaValidator.validateSchema(contentSchemaFileName,getContentNode(reportJsonNode),contentSchemaFile,objectMapper,invalidData)
            if(!validationSchemaResult.status) return  validationSchemaResult
        }
        catch (e: BadRequestException) {
            logger.error("The report validation failed ${e.message}")
            throw e
        }catch (e: MalformedException){
           val reason = "Report rejected: Malformed JSON or error processing the report"
            e.message?.let { invalidData.add(it) }
            //TODO :This should be done in the calling code
           // val malformedReportMessage = safeParseMessageAsReport(message)
           return errorProcessor.processError(
                reason,
                invalidData)
        }
       return ValidationSchemaResult("Successfully validated the report schema", true, mutableListOf())
    }

    private fun loadAndVerifyBaseSchema(message:String, nodeText: String, invalidData:MutableList<String>):Pair<File?,ValidationSchemaResult?>{
        //for backward compatibility following schema version will be loaded if report_schema_version is not found
        val defaultSchemaVersion = "0.0.1"
        var validationSchemaResult:ValidationSchemaResult? = null
        // get schema version, and use appropriate base schema version
        val reportSchemaVersion = jsonUtils.getSchemaVersion(message,nodeText) ?: defaultSchemaVersion
        logger.info("The version of schema report $reportSchemaVersion")
        val baseSchemaFileName = "base.$reportSchemaVersion.schema.json"
        // load schema file
        val schemaFile = schemaLoader.loadSchemaFile(baseSchemaFileName)
         if(schemaFile == null){
             validationSchemaResult=  errorProcessor.processError(
                 "Report rejected: Schema file not found for base schema version $reportSchemaVersion",
                 invalidData)
         }
             return Pair(schemaFile, validationSchemaResult)

    }

    private fun validateContentType(reportJsonNode: JsonNode, invalidData:MutableList<String>):ValidationSchemaResult{
        //check for content type node
        val contentTypeNode = reportJsonNode.get("content_type")
        var reason="Content Type node is valid"
        if (contentTypeNode == null) {
           reason = "Report rejected: `content_type` is not JSON or is missing"

        } else {
            if (!jsonUtils.isJsonMimeType(contentTypeNode.asText())) {
                // Don't need to go further down if the mimetype is other than json. i.e. xml or text etc.
                reason = "Don't need to go further down the schema, since the mimetype is not json."
            }
            else{
                return ValidationSchemaResult(reason,true, invalidData)
            }
        }
        return errorProcessor.processError(
            reason,
            invalidData)
    }


    private fun validateContent(reportJsonNode: JsonNode, invalidData:MutableList<String>):ValidationSchemaResult{
        //check for content node
        val contentNode = reportJsonNode.get("content")
        var reason="Content node is valid"
        if (contentNode == null) {
            reason = "Report rejected: `content` is not JSON or is missing."
            return errorProcessor.processError(
                reason,
                invalidData)
        }
        return ValidationSchemaResult(reason,true, invalidData)
    }

    private fun validateContentSchemaNodeVersion(reportJsonNode: JsonNode, invalidData:MutableList<String>):ValidationSchemaResult{
        //check for `content_schema_name` and `content_schema_version`
        var reason="Content Schema Name and Content Schema Version are valid"
        val contentNode = getContentNode(reportJsonNode)
        val contentSchemaNameNode = getContentSchemaNameNode(contentNode)
        val contentSchemaVersionNode = getContentSchemaVersionNode(contentNode)
        if (contentSchemaNameNode == null || contentSchemaNameNode.asText().isEmpty() ||
            contentSchemaVersionNode == null || contentSchemaVersionNode.asText().isEmpty()
        ) {
            reason = "Report rejected: `content_schema_name` or `content_schema_version` is missing or empty."
            return errorProcessor.processError(
                reason,
                invalidData)
        }
        return ValidationSchemaResult(reason,true, invalidData)
    }

    private fun validateContentSchemaFile(reportJsonNode: JsonNode, invalidData:MutableList<String>):Pair<File?,ValidationSchemaResult>{
        val reason: String
        var status = false
        val contentNode = getContentNode(reportJsonNode)
        //proceed with content validation
        val contentSchemaName = getContentSchemaNameNode(contentNode)!!.asText()
        val contentSchemaVersion = getContentSchemaVersionNode(contentNode)!!.asText()
        val contentSchemaFileName = "$contentSchemaName.$contentSchemaVersion.schema.json"
        val contentSchemaFilePath = schemaLoader.loadSchemaFile(contentSchemaFileName)
        val contentSchemaFile = if (contentSchemaFilePath != null) File(contentSchemaFilePath.toURI()) else null

        if (contentSchemaFile == null || !contentSchemaFile.exists()) {
            reason =
                "Report rejected: Content schema file not found for content schema name '$contentSchemaName' and schema version '$contentSchemaVersion'."
            errorProcessor.processError(
                reason,
                invalidData)

        }
        else{
            status=true
            reason="Content schema file found for content schema name '$contentSchemaName' and schema version '$contentSchemaVersion'"

        }
        return Pair(contentSchemaFile,ValidationSchemaResult(reason,status, invalidData))
    }

    private fun getContentNode(reportJsonNode:JsonNode):JsonNode{
        return reportJsonNode.get("content")
    }

    private fun getContentSchemaNameNode(contentNode: JsonNode):JsonNode?{
       return contentNode.get("content_schema_name")
    }

    private fun getContentSchemaVersionNode(contentNode: JsonNode):JsonNode?{
        return contentNode.get("content_schema_version")
    }
}