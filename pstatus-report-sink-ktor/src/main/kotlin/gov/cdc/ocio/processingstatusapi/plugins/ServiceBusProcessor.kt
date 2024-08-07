package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.messaging.servicebus.ServiceBusReceivedMessage
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.processingstatusapi.ReportManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.reports.CreateReportSBMessage
import gov.cdc.ocio.processingstatusapi.models.reports.Source
import mu.KotlinLogging
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import java.awt.datatransfer.MimeTypeParseException
import java.io.File
import javax.activation.MimeType

/**
 * The service bus is another interface for receiving reports.
 *
 * @property logger KLogger
 * @property gson (Gson..Gson?)
 */
class ServiceBusProcessor {

    private val logger = KotlinLogging.logger {}

    // Use the LONG_OR_DOUBLE number policy, which will prevent Longs from being made into Doubles
    private val gson = GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create()

    /**
     * Process a service bus message with the provided message.
     *
     * @param message String
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    fun withMessage(message: ServiceBusReceivedMessage) {
        val sbMessageId = message.messageId
        var sbMessage =String(message.body.toBytes())
        val sbMessageStatus = message.state.name

        try {
            logger.info { "Before Message received = $sbMessage" }
            if (sbMessage.contains("destination_id")) {
                sbMessage = sbMessage.replace("destination_id", "data_stream_id")
            }
            if (sbMessage.contains("event_type")) {
                sbMessage = sbMessage.replace("event_type", "data_stream_route")
            }
            logger.info { "After Message received = $sbMessage" }
            val disableValidation = System.getenv("DISABLE_VALIDATION")?.toBoolean() ?: false

            if (disableValidation) {
                val isValid = isJsonValid(sbMessage)
                if (!isValid)
                    sendToDeadLetter("Validation failed.  The message is not in JSON format.")
            }
            else
                validateJsonSchema(message)

            createReport(sbMessageId, sbMessageStatus, gson.fromJson(sbMessage, CreateReportSBMessage::class.java))
        } catch (e: BadRequestException) {
            logger.error("Validation failed: ${e.message}")
            throw e
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse service bus message: ${e.localizedMessage}")
            throw BadStateException("Unable to interpret the create report message")
        }
    }

    /**
     * Create a report from the provided service bus message.
     *
     * @param createReportMessage CreateReportSBMessage
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    private fun createReport(messageId: String, messageStatus: String, createReportMessage: CreateReportSBMessage) {
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
                createReportMessage.messageMetadata,
                createReportMessage.stageInfo,
                createReportMessage.tags,
                createReportMessage.data,
                createReportMessage.contentType!!,
                messageId, //createReportMessage.messageId is null
                messageStatus, //createReportMessage.status is null
                createReportMessage.content!!, // it was Content I changed to ContentAsString
                createReportMessage.jurisdiction,
                createReportMessage.senderId,
                createReportMessage.dispositionType,
                Source.SERVICEBUS
            )
        } catch (e: BadRequestException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to process service bus message:${e.message}")
        }
    }

    /**
     * Function to validate report attributes for missing required fields, for schema validation and malformed content message using networknt/json-schema-validator
     * @param message ServiceBusReceivedMessage
     * @throws BadRequestException
     */
    private fun validateJsonSchema(message: ServiceBusReceivedMessage){
        val invalidData = mutableListOf<String>()
        val schemaFileNames = mutableListOf<String>()
        var reason: String
        //TODO : this needs to be replaced with more robust source or a URL of some sorts
        val schemaDirectoryPath = "/schema"
        // Convert the message body to a JSON string
        val messageBody = String(message.body.toBytes())
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        var reportSchemaVersion = "0.0.1" // for backward compatibility - this schema will load if report_schema_version is not found

        // Check for the presence of `report_schema_version`
        try {
            val createReportMessage: CreateReportSBMessage = gson.fromJson(messageBody, CreateReportSBMessage::class.java)
            // Convert to JSON
            val jsonNode: JsonNode =objectMapper.readTree(messageBody)
            // Check for the presence of `report_schema_version`
            val reportSchemaVersionNode = jsonNode.get("report_schema_version")
            if (reportSchemaVersionNode == null || reportSchemaVersionNode.asText().isEmpty()) {
                logger.info("Report schema version node missing. Backward compatibility mode enabled ")
            } else {
                reportSchemaVersion = reportSchemaVersionNode.asText()
            }
            val fileName ="base.$reportSchemaVersion.schema.json"
            val schemaFilePath = javaClass.getResource( "$schemaDirectoryPath/$fileName")
                ?: throw IllegalArgumentException("File not found: $fileName")

            // Attempt to load the schema
            val schemaFile = File(schemaFilePath.toURI())
            if (!schemaFile.exists()) {
                reason ="Report rejected: Schema file not found for base schema version $reportSchemaVersion."
                processError(fileName,reason,invalidData,schemaFileNames,createReportMessage)
            }
            // Validate report schema version schema
            validateSchema(fileName,jsonNode,schemaFile,objectMapper,invalidData,schemaFileNames,createReportMessage)
            // Check if the content_type is JSON
            val contentTypeNode = jsonNode.get("content_type")
            if (contentTypeNode == null) {
                reason="Report rejected: `content_type` is not JSON or is missing."
                processError(fileName,reason,invalidData,schemaFileNames,createReportMessage)
            }
            else {
                if (!isJsonMimeType(contentTypeNode.asText())) {
                    // Don't need to go further down if the mimetype is other than json. i.e. xml or text etc.
                    return
                }
            }

            // Open the content as JSON
            val contentNode = jsonNode.get("content")
            if (contentNode == null) {
                reason="Report rejected: `content` is not JSON or is missing."
                processError(fileName,reason,invalidData, schemaFileNames,createReportMessage)
            }
            // Check for `content_schema_name` and `content_schema_version`
            val contentSchemaNameNode = contentNode.get("content_schema_name")
            val contentSchemaVersionNode = contentNode.get("content_schema_version")
            if (contentSchemaNameNode == null || contentSchemaNameNode.asText().isEmpty() ||
                contentSchemaVersionNode == null || contentSchemaVersionNode.asText().isEmpty()
            ) {
                reason= "Report rejected: `content_schema_name` or `content_schema_version` is missing or empty."
                processError(fileName,reason,invalidData, schemaFileNames,createReportMessage)
            }
            //ContentSchema validation
            val contentSchemaName = contentSchemaNameNode.asText()
            val contentSchemaVersion = contentSchemaVersionNode.asText()
            val contentSchemaFileName ="$contentSchemaName.$contentSchemaVersion.schema.json"
            val contentSchemaFilePath =javaClass.getResource( "$schemaDirectoryPath/$contentSchemaFileName")
                ?: throw IllegalArgumentException("File not found: $contentSchemaFileName")

            // Attempt to load the schema
            val contentSchemaFile = File(contentSchemaFilePath.toURI())
            if (!contentSchemaFile .exists()) {
                reason ="Report rejected: Content schema file not found for content schema name $contentSchemaName and schema version $contentSchemaVersion."
                processError(contentSchemaFileName,reason,invalidData,schemaFileNames,createReportMessage)
            }

            // Validate content schema
            validateSchema(contentSchemaFileName,contentNode,contentSchemaFile,objectMapper,invalidData, schemaFileNames, createReportMessage)

        } catch (e: Exception) {
            logger.error("Report rejected: Malformed JSON or error processing the report - ${e.message}")
            throw e
        }
    }

    /**
     *  Function to send the invalid data reasons to the deadLetter queue
     *  @param invalidData MutableList<String>
     *  @param createReportMessage CreateReportSBMessage
     */
    private fun sendToDeadLetter(invalidData:MutableList<String>, validationSchemaFileNames:MutableList<String>, createReportMessage: CreateReportSBMessage){
        if (invalidData.isNotEmpty()) {
            //This should not run for unit tests
            if (System.getProperty("isTestEnvironment") != "true") {
                // Write the content of the dead-letter reports to CosmosDb
                ReportManager().createDeadLetterReport(
                    createReportMessage.uploadId,
                    createReportMessage.dataStreamId,
                    createReportMessage.dataStreamRoute,
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

    /**
     *  Function to send the invalid data reasons to the deadLetter queue
     *  @param reason String
     */
    private fun sendToDeadLetter(reason:String){
        //This should not run for unit tests
        if (System.getProperty("isTestEnvironment") != "true") {
            // Write the content of the dead-letter reports to CosmosDb
            ReportManager().createDeadLetterReport(reason)
            throw BadRequestException(reason)
        }
    }

    /**
     *  Function to process the error by logging it and adding to the invalidData list and sending it to deadletter
     *  @param reason String
     *  @param invalidData MutableList<String>
     *  @param createReportMessage CreateReportSBMessage
     */
    private fun processError(schemaFileName:String, reason:String, invalidData:MutableList<String>, validationSchemaFileNames:MutableList<String>,
                             createReportMessage: CreateReportSBMessage) {

        validationSchemaFileNames.add(schemaFileName)
        val updatedReason = reason +  "Filename(s) used for validation: " + validationSchemaFileNames.joinToString(separator = ",")
        logger.error(updatedReason)
        invalidData.add(updatedReason)
        sendToDeadLetter(invalidData, validationSchemaFileNames, createReportMessage)
    }

    /**
     *  Function to validate the schema based on the schema file and the json contents passed into it
     *  @param schemaFile String
     *  @param objectMapper ObjectMapper

     */
    private fun validateSchema(schemaFileName: String, jsonNode:JsonNode, schemaFile:File, objectMapper: ObjectMapper, invalidData:MutableList<String>,
                               validationSchemaFileNames: MutableList<String>, createReportMessage: CreateReportSBMessage) {
        val schemaNode: JsonNode = objectMapper.readTree(schemaFile)
        val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
        val schema: JsonSchema = schemaFactory.getSchema(schemaNode)
        val schemaValidationMessages: Set<ValidationMessage> = schema.validate(jsonNode)

        if (schemaValidationMessages.isEmpty()) {
            logger.info("JSON is valid against the content schema $schema.")
        } else {
            val reason ="JSON is invalid against the content schema $schemaFileName."
            schemaValidationMessages.forEach { invalidData.add(it.message) }
            processError(schemaFileName,reason, invalidData,validationSchemaFileNames,createReportMessage)
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

    /**
     *  Function to check whether the message is in JSON format or not
     */
    private fun isJsonValid(jsonString: String): Boolean {
        return try {
            val mapper = jacksonObjectMapper()
            mapper.readTree(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }

}