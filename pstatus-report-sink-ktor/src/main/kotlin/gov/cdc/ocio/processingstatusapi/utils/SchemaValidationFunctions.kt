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
import gov.cdc.ocio.processingstatusapi.models.reports.CreateReportSBMessage
import gov.cdc.ocio.processingstatusapi.models.reports.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.reports.Source
import gov.cdc.ocio.processingstatusapi.models.reports.StageInfo
import gov.cdc.ocio.processingstatusapi.utils.Helpers.logger
import gov.cdc.ocio.processingstatusapi.utils.Helpers.reason

import mu.KotlinLogging
import java.awt.datatransfer.MimeTypeParseException
import java.io.File
import java.time.Instant
import java.util.*
import javax.activation.MimeType



object Helpers {
    //Use the LONG_OR_DOUBLE number policy, which will prevent Longs from being made into Doubles
    val gson: Gson = GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create()
    val logger = KotlinLogging.logger {}

    /**
     * Loads a base schema file from resources folder and returns file object.
     * @param schemaDirectoryPath The directory path where schema file is located.
     * @param fileName The name of the schema file to load.
     */
    fun loadSchemaFile(schemaDirectoryPath: String, fileName: String): File?{
        val schemaFilePath = javaClass.classLoader.getResource("$schemaDirectoryPath/$fileName")
        return schemaFilePath?.let { File(it.toURI()) }?.takeIf { it.exists() }

    }

    lateinit var reason: String
}


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
 * Checks for depreciated fields within message that are still accepted for backward compatability.
 * If any depreciated fields are found, they are replaced with their corresponding new fields.
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
 * Function to validate report attributes for missing required fields, for schema validation and malformed content message using networknt/json-schema-validator
 * @param message ReceivedMessage(from Azure Service Bus, RabbitMQ Queue or AWS SNS/SQS)
 * @throws BadRequestException
 */
fun validateJsonSchema(message: String) {
    val invalidData = mutableListOf<String>()
    val schemaFileNames = mutableListOf<String>()
    val schemaDirectoryPath = "schema"
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    //for backward compatability following schema version will be loaded if report_schema_version is not found
    val defaultSchemaVersion = "0.0.1"

    val createReportMessage: CreateReportSBMessage
    try {
        createReportMessage = Helpers.gson.fromJson(message, CreateReportSBMessage::class.java)
        //convert to Json
        val reportJsonNode = objectMapper.readTree(message)
        // get schema version, and use appropriate base schema version
        val reportSchemaVersion = getSchemaVersion(message)?:defaultSchemaVersion
        val baseSchemaFileName = "base.$reportSchemaVersion.schema.json"

        val schemaFile = Helpers.loadSchemaFile(schemaDirectoryPath, baseSchemaFileName)?: return processError(
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
        val contentSchemaFilePath = Helpers.loadSchemaFile(schemaDirectoryPath, contentSchemaFileName)

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
@Throws(BadRequestException::class)
fun createReport(createReportMessage: CreateReportSBMessage) {
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
            createReportMessage.dispositionType,
            Source.SERVICEBUS
        )
    } catch (e: BadRequestException) {
        throw e
    } catch (e: Exception) {
        logger.error("createReport - Failed to process message:${e.message}")
    }
}
/**
 *  Function to send the invalid data reasons to the deadLetter queue.
 *
 *  @param reason String
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
 * Function to check whether the content type is json or application/json using MimeType
 * @param contentType String
 */
private fun isJsonMimeType(contentType: String): Boolean {
    return try {
        val mimeType = MimeType(contentType)
        mimeType.primaryType == "json" || (mimeType.primaryType == "application" && mimeType.subType == "json")
    } catch (e: MimeTypeParseException) {
        false
    }
}
fun sendToDeadLetter(
    invalidData: MutableList<String>,
    validationSchemaFileNames: MutableList<String>,
    createReportMessage: CreateReportSBMessage
) {
    if (invalidData.isNotEmpty()) {
        //This should not run for unit tests
        if (System.getProperty("isTestEnvironment") != "true") {
            // Write the content of the dead-letter reports to CosmosDb
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
                invalidData,
                validationSchemaFileNames
            )
        }
        throw BadRequestException(invalidData.joinToString(separator = ","))
    }
}

fun getSchemaVersion(message: String) :String?{
    val jsonNode = jacksonObjectMapper().readTree(message)
    return jsonNode.get("report_schema_version")?.asText().takeIf { !it.isNullOrEmpty() }

}
/**
 * Attempts to safely parse the message body into a report for deadlettering.  Note, this is needed when the
 * content may be malformed, such as the structure not being valid JSON or elements not of the expected type.
 *
 * @param messageBody String
 * @return CreateReportSBMessage
 */
fun safeParseMessageAsReport(messageBody: String): CreateReportSBMessage {
    val objectMapper = jacksonObjectMapper()
    val jsonNode = objectMapper.readTree(messageBody)
    val malformedReportSBMessage = CreateReportSBMessage().apply {
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

        contentType = runCatching { jsonNode.get("content_type") }.getOrNull()?.asText()
        // Try to get the content as JSON object, but if not, get it as a string
        val contentAsNode = runCatching { jsonNode.get("content") }.getOrNull()
        content = runCatching { when (contentAsNode?.nodeType) {
            JsonNodeType.OBJECT -> objectMapper.convertValue(contentAsNode, object : TypeReference<Map<*, *>>() {})
            else -> contentAsNode?.asText()
        }}.getOrNull()
    }
    return malformedReportSBMessage
}

fun validateSchemaContent(schemaFileName: String, jsonNode: JsonNode, schemaFile: File, objectMapper: ObjectMapper,
                          invalidData: MutableList<String>, validationSchemaFileNames: MutableList<String>, createReportMessage: CreateReportSBMessage) {
    val schemaNode: JsonNode = objectMapper.readTree(schemaFile)
    val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
    val schema: JsonSchema = schemaFactory.getSchema(schemaNode)
    val schemaValidationMessages: Set<ValidationMessage> = schema.validate(jsonNode)
    println ("schema node $schemaNode factory $schemaFactory schema $schema  schemaValidationMessages $schemaValidationMessages" )
    if (schemaValidationMessages.isEmpty()) {
        logger.info("JSON is valid against the content schema $schema.")
    } else {
        val reason ="JSON is invalid against the content schema $schemaFileName."
        schemaValidationMessages.forEach { invalidData.add(it.message) }
        processError(reason, invalidData,validationSchemaFileNames,createReportMessage)
    }


}

/**
 * Function to process the error by logging it and adding to the invalidData list and sending it to deadletter.
 *
 * @param reason String
 * @param invalidData MutableList<String>
 * @param validationSchemaFileNames MutableList<String>
 * @param createReportMessage CreateReportSBMessage
 */
private fun processError(
    reason: String,
    invalidData: MutableList<String>,
    validationSchemaFileNames: MutableList<String>,
    createReportMessage: CreateReportSBMessage
) {
    logger.error(reason)
    invalidData.add(reason)
    sendToDeadLetter(invalidData, validationSchemaFileNames, createReportMessage)
}
