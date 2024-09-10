package gov.cdc.ocio.processingstatusapi.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import gov.cdc.ocio.processingstatusapi.ReportManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.reports.CreateReportMessage
import gov.cdc.ocio.processingstatusapi.models.reports.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.reports.Source
import gov.cdc.ocio.processingstatusapi.models.reports.StageInfo
import mu.KotlinLogging
import java.awt.datatransfer.MimeTypeParseException
import java.io.File
import java.time.Instant
import java.util.*
import javax.activation.MimeType

/**
 * Utility class for validating reports against predefined schemas. It's intended to be re-used across supported
 * messaging systems: Azure Service Bus, RabbitMQ and AWS SQS to ensure consistent schema validation and error handling.
 */
class SchemaValidation {
    companion object {
        //Use the LONG_OR_DOUBLE number policy, which will prevent Longs from being made into Doubles
        val gson: Gson = GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create()
        val logger = KotlinLogging.logger {}
        lateinit var reason: String
    }
    /**
     * Loads a base schema file from resources folder and returns file object.
     * @param fileName The name of the schema file to load.
     */
    private fun loadSchemaFile(fileName: String): File?{
        val schemaDirectoryPath = "schema"
        val schemaFilePath = javaClass.classLoader.getResource("$schemaDirectoryPath/$fileName")
        return schemaFilePath?.let { File(it.toURI()) }?.takeIf { it.exists() }

    }

    /**
     * Function to validate report attributes for missing required fields, for schema validation and malformed content message using networknt/json-schema-validator.
     *
     * @param message ReceivedMessage(from Azure Service Bus, RabbitMQ Queue or AWS SNS/SQS)
     * @throws BadRequestException
     */
    fun validateJsonSchema(message: String) {
        val invalidData = mutableListOf<String>()
        val schemaFileNames = mutableListOf<String>()
        val objectMapper: ObjectMapper = jacksonObjectMapper()

        //for backward compatability following schema version will be loaded if report_schema_version is not found
        val defaultSchemaVersion = "0.0.1"

        val createReportMessage: CreateReportMessage
        try {
            createReportMessage = gson.fromJson(message, CreateReportMessage::class.java)
            //convert to Json
            val reportJsonNode = objectMapper.readTree(message)
            // get schema version, and use appropriate base schema version
            val reportSchemaVersion = getSchemaVersion(message)?:defaultSchemaVersion
            logger.info ("The version of schema report $reportSchemaVersion")
            val baseSchemaFileName = "base.$reportSchemaVersion.schema.json"

            val schemaFile = loadSchemaFile(baseSchemaFileName)?: return processError(
                "Report rejected: Schema file not found for base schema version $reportSchemaVersion",
                invalidData,
                schemaFileNames,
                createReportMessage)

            schemaFileNames.add(baseSchemaFileName)
            //validate base schema version
            validateSchemaContent(schemaFile.name,reportJsonNode, schemaFile,objectMapper, invalidData, schemaFileNames, createReportMessage)
            //check for content type

            val contentTypeNode = reportJsonNode.get("content_type")
            if (contentTypeNode == null) {
                reason = "Report rejected: `content_type` is not JSON or is missing"
                processError(
                    reason,
                    invalidData,
                    schemaFileNames,
                    createReportMessage)
            }else{
                if (!isJsonMimeType(contentTypeNode.asText())) {
                    // Don't need to go further down if the mimetype is other than json. i.e. xml or text etc.
                    return
                }
            }
            // Open the content as JSON
            val contentNode = reportJsonNode.get("content")
            reason = "Report rejected: `content` is not JSON or is missing."
            if (contentNode == null) {
                processError(
                    reason,
                    invalidData,
                    schemaFileNames,
                    createReportMessage)
            }
            //check for `content_schema_name` and `content_schema_version`
            val contentSchemaNameNode = contentNode.get("content_schema_name")
            val contentSchemaVersionNode = contentNode.get("content_schema_version")
            if (contentSchemaNameNode == null || contentSchemaNameNode.asText().isEmpty() ||
                contentSchemaVersionNode == null || contentSchemaVersionNode.asText().isEmpty()
            ) {
                reason = "Report rejected: `content_schema_name` or `content_schema_version` is missing or empty."
                processError(
                    reason,
                    invalidData,
                    schemaFileNames,
                    createReportMessage)
            }

            //proceed with content validation
            val contentSchemaName = contentSchemaNameNode.asText()
            val contentSchemaVersion = contentSchemaVersionNode.asText()
            val contentSchemaFileName = "$contentSchemaName.$contentSchemaVersion.schema.json"
            val contentSchemaFilePath = SchemaValidation().loadSchemaFile(contentSchemaFileName)

            val contentSchemaFile = if (contentSchemaFilePath !=null) File(contentSchemaFilePath.toURI()) else null
            if (contentSchemaFile == null || !contentSchemaFile.exists()) {
                reason = "Report rejected: Content schema file not found for content schema name '$contentSchemaName' and schema version '$contentSchemaVersion'."
                processError(
                    reason,
                    invalidData,
                    schemaFileNames,
                    createReportMessage)
            }
            schemaFileNames.add(contentSchemaFileName)
            //validate content schema
            validateSchemaContent(
                contentSchemaName,
                contentNode,
                contentSchemaFile!!,
                objectMapper,
                invalidData,
                schemaFileNames,
                createReportMessage)
        }catch (e: Exception){
            reason = "Report rejected: Malformed JSON or error processing the report"
            e.message?.let { invalidData.add(it) }
            val malformedReportSBMessage = safeParseMessageAsReport(message)
            processError(reason, invalidData, schemaFileNames, malformedReportSBMessage)
        }
    }

    /**
     * This function parses the provided message and attempts to extract `report_schema_version` field.
     * @param message message to be parsed
     * @return value for `report_schema_version` field if found, otherwise `null`
     *
     */
    private fun getSchemaVersion(message: String) :String?{
        val jsonNode = jacksonObjectMapper().readTree(message)
        return jsonNode.get("report_schema_version")?.asText().takeIf { !it.isNullOrEmpty() }

    }

    /**
     * Attempts to safely parse the message body into a report for dead-lettering.  Note, this is needed when the
     * content may be malformed, such as the structure not being valid JSON or elements not of the expected type.
     *
     * @param messageBody String
     * @return CreateReportSBMessage The message that contains details about the report to be processed
     *   The message may come from  Azure Service Bus, AWS SQS or RabbitMQ.
     */
    private fun safeParseMessageAsReport(messageBody: String): CreateReportMessage {
        val objectMapper = jacksonObjectMapper()
        val jsonNode = objectMapper.readTree(messageBody)
        val malformedReportMessage = CreateReportMessage().apply {
            // Attempt to get each element of the json structure if available
            uploadId = runCatching { jsonNode.get("upload_id") }.getOrNull()?.asText()
            dataStreamId = runCatching { jsonNode.get("data_stream_id") }.getOrNull()?.asText()
            dataStreamRoute = runCatching { jsonNode.get("data_stream_route") }.getOrNull()?.asText()
            dexIngestDateTime = runCatching {
                val dexIngestDateTimeStr = jsonNode.get("dex_ingest_datetime").asText()
                val dexIngestDateTimeInstant = Instant.parse(dexIngestDateTimeStr)
                Date.from(dexIngestDateTimeInstant)
            }.getOrNull()

            // Try to get the metadata as JSON object, but if not, get it as a string
            val messageMetadataAsNode = runCatching { jsonNode.get("message_metadata") }.getOrNull()
            messageMetadata = runCatching { when (messageMetadataAsNode?.nodeType) {
                JsonNodeType.OBJECT -> objectMapper.convertValue(messageMetadataAsNode, object : TypeReference<MessageMetadata>() {})
                else -> null
            }}.getOrNull()

            // Try to get the stage info as JSON object, but if not, get it as a string
            val stageInfoAsNode = runCatching { jsonNode.get("stage_info") }.getOrNull()
            stageInfo = runCatching { when (stageInfoAsNode?.nodeType) {
                JsonNodeType.OBJECT -> objectMapper.convertValue(stageInfoAsNode, object : TypeReference<StageInfo>() {})
                else -> null
            }}.getOrNull()

            // Try to get the tags as JSON object, but if not, get it as a string
            val tagsAsNode = runCatching { jsonNode.get("tags") }.getOrNull()
            tags = runCatching { when (tagsAsNode?.nodeType) {
                JsonNodeType.OBJECT -> objectMapper.convertValue(tagsAsNode, object : TypeReference<Map<String, String>>() {})
                else -> null
            }}.getOrNull()

            // Try to get the data as JSON object, but if not, get it as a string
            val dataAsNode = runCatching { jsonNode.get("data") }.getOrNull()
            data = runCatching { when (dataAsNode?.nodeType) {
                JsonNodeType.OBJECT -> objectMapper.convertValue(dataAsNode, object : TypeReference<Map<String, String>>() {})
                else -> null
            }}.getOrNull()

            jurisdiction = runCatching { jsonNode.get("jurisdiction") }.getOrNull()?.asText()
            senderId = runCatching { jsonNode.get("sender_id") }.getOrNull()?.asText()
            dataProducerId = runCatching { jsonNode.get("data_producer_id") }.getOrNull()?.asText()
            contentType = runCatching { jsonNode.get("content_type") }.getOrNull()?.asText()
            // Try to get the content as JSON object, but if not, get it as a string
            val contentAsNode = runCatching { jsonNode.get("content") }.getOrNull()
            content = runCatching { when (contentAsNode?.nodeType) {
                JsonNodeType.OBJECT -> objectMapper.convertValue(contentAsNode, object : TypeReference<Map<*, *>>() {})
                else -> contentAsNode?.asText()
            }}.getOrNull()
        }
        return malformedReportMessage
    }

    /**
     * Function validates the content of the JSON node against specified JSON Schema using
     * `networknt` JSON schema validator and calls processError() for further processing failed node.
     *
     * @param schemaFileName The name of the JSON schema file used for validation.
     * @param jsonNode The node containing data to be validated.
     * @param schemaFile The schema file read from `schemaFilePath`
     * @param objectMapper The ObjectMapper used to read and parse the schema file.
     * @param invalidData The list with validation error messages.
     * @param validationSchemaFileNames The base schema file names used during the validation process
     * @param createReportMessage The message that contains details about the report to be processed
     *     The message may come from  Azure Service Bus, AWS SQS or RabbitMQ.
     *
     */
    private fun validateSchemaContent(schemaFileName: String, jsonNode: JsonNode, schemaFile: File, objectMapper: ObjectMapper,
                              invalidData: MutableList<String>, validationSchemaFileNames: MutableList<String>, createReportMessage: CreateReportMessage
    ) {
        logger.info("Schema file base: $schemaFileName")
        logger.info("schemaFileNames: $validationSchemaFileNames")
        val schemaNode: JsonNode = objectMapper.readTree(schemaFile)
        val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
        val schema: JsonSchema = schemaFactory.getSchema(schemaNode)
        val schemaValidationMessages: Set<ValidationMessage> = schema.validate(jsonNode)

        if (schemaValidationMessages.isEmpty()) {
            logger.info("The report has been successfully validated against the JSON schema:$schemaFileName.")
        } else {
            val reason ="The report could not be validated against the JSON schema: $schemaFileName."
            schemaValidationMessages.forEach { invalidData.add(it.message) }
            processError(reason, invalidData,validationSchemaFileNames,createReportMessage)
        }
    }
    /**
     * Checks for depreciated fields within message that are still accepted for backward compatability.
     * If any depreciated fields are found, they are replaced with their corresponding new fields.
     *
     * @param messageAsString message to be checked against depreciated fields.
     * @return updated message if depreciated fields were found.
     */
    fun checkAndReplaceDeprecatedFields(messageAsString: String): String {
        var message = messageAsString
        if (message.contains("destination_id")) {
            message= message.replace("destination_id", "data_stream_id")
        }
        if (message.contains("event_type")) {
            message = message.replace("event_type", "data_stream_route")
        }
        return message
    }

    /**
     * Creates report using ReportManager() and persists to Cosmos DB.
     *
     * @param createReportMessage The message that contains details about the report to be processed
     * The message may come from Azure Service Bus, AWS SQS or RabbitMQ.
     * @throws BadRequestException
     * @throws Exception
     */
    fun createReport(createReportMessage: CreateReportMessage) {
        try {
            val uploadId = createReportMessage.uploadId
            var stageName = createReportMessage.stageInfo?.action
            if (stageName.isNullOrEmpty()) {
                stageName = ""
            }
            logger.info("Creating report for uploadId = $uploadId with stageName = $stageName")

            ReportManager().createReportWithUploadId(
                uploadId!!,
                createReportMessage.dataStreamId!!,
                createReportMessage.dataStreamRoute!!,
                createReportMessage.dexIngestDateTime!!,
                createReportMessage.messageMetadata,
                createReportMessage.stageInfo,
                createReportMessage.tags,
                createReportMessage.data,
                createReportMessage.contentType!!,
                createReportMessage.content!!, // it was Content I changed to ContentAsString
                createReportMessage.jurisdiction,
                createReportMessage.senderId,
                createReportMessage.dataProducerId,
                createReportMessage.dispositionType,
                Source.SERVICEBUS
            )
        } catch (e: BadRequestException) {
            logger.error("createReport - bad request exception: ${e.message}")
        } catch (e: Exception) {
            logger.error("createReport - Failed to process message:${e}")
        }
    }
    /**
     *  Sends invalid report to dead-letter container in Cosmos DB.
     *
     *  @param reason String that explains the failure
     *  @throws BadRequestException
     */
    fun sendToDeadLetter(reason:String){
        //This should not run for unit tests
        if (System.getProperty("isTestEnvironment") != "true") {
            // Write the content of the dead-letter reports to CosmosDb
            ReportManager().createDeadLetterReport(reason)
            throw BadRequestException(reason)
        }
    }

    /**
     * Creates report and Sends to dead-letter container in Cosmos DB.
     *
     * @param invalidData list of reason(s) why report failed
     * @param validationSchemaFileNames schema files used during validation process
     * @param createReportMessage The message that contains details about the report to be processed
     * The message may come from  Azure Service Bus, AWS SQS or RabbitMQ.
     * @throws BadRequestException
     */
    private fun sendToDeadLetter(
        invalidData: MutableList<String>,
        validationSchemaFileNames: MutableList<String>,
        createReportMessage: CreateReportMessage
    ) {
        if (invalidData.isNotEmpty()) {
            //This should not run for unit tests
            if (System.getProperty("isTestEnvironment") != "true") {
                ReportManager().createDeadLetterReport(
                    createReportMessage.uploadId,
                    createReportMessage.dataStreamId,
                    createReportMessage.dataStreamRoute,
                    createReportMessage.dexIngestDateTime,
                    createReportMessage.messageMetadata,
                    createReportMessage.stageInfo,
                    createReportMessage.tags,
                    createReportMessage.data,
                    createReportMessage.dispositionType,
                    createReportMessage.contentType,
                    createReportMessage.content,
                    createReportMessage.jurisdiction,
                    createReportMessage.senderId,
                    createReportMessage.dataProducerId,
                    invalidData,
                    validationSchemaFileNames
                )
            }
            throw BadRequestException(invalidData.joinToString(separator = ","))
        }
    }

    /**
     * Function to process the error by logging it and adding to the invalidData list and sending it to deadletter.
     *
     * @param reason String
     * @param invalidData MutableList<String>
     * @param validationSchemaFileNames MutableList<String>
     * @param createReportMessage The message that contains details about the report to be sent to dead-letter
     *     The message may come from  Azure Service Bus, AWS SQS or RabbitMQ.
     */
    private fun processError(
        reason: String,
        invalidData: MutableList<String>,
        validationSchemaFileNames: MutableList<String>,
        createReportMessage: CreateReportMessage
    ) {
        logger.error(reason)
        invalidData.add(reason)
        sendToDeadLetter(invalidData, validationSchemaFileNames, createReportMessage)
    }


    /**
     * Function checks if input string is a valid JSON
     * @param jsonString to be checked
     * @throws Exception
     * @return boolean true if its valid JSON otherwise false
     */
    fun isJsonValid(jsonString: String): Boolean {
        return try {
            val mapper = jacksonObjectMapper()
            mapper.readTree(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Function to check whether the content type is json or application/json using MimeType.
     * @param contentType string
     * @throws MimeTypeParseException
     * @return boolean true if mimeType is `json` or `application/json` otherwise false
     */
    private fun isJsonMimeType(contentType: String): Boolean {
        return try {
            val mimeType = MimeType(contentType)
            mimeType.primaryType == "json" || (mimeType.primaryType == "application" && mimeType.subType == "json")
        } catch (e: MimeTypeParseException) {
            false
        }
    }


}