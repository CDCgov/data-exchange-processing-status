package gov.cdc.ocio.processingstatusapi.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.database.utils.InstantTypeAdapter
import gov.cdc.ocio.processingstatusapi.ReportManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.CreateReportMessage
import gov.cdc.ocio.processingstatusapi.models.Source
import mu.KotlinLogging
import java.time.Instant

/**
 * Utility class for validating reports against predefined schemas. It's intended to be re-used across supported
 * messaging systems: Azure Service Bus, RabbitMQ and AWS SQS to ensure consistent schema validation and error handling.
 */
class SchemaValidation {
    companion object {
        //Use the LONG_OR_DOUBLE number policy, which will prevent Longs from being made into Doubles
        val gson: Gson = GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
        val logger = KotlinLogging.logger {}
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
    fun createReport(createReportMessage: CreateReportMessage, source: Source) {
        try {
            val uploadId = createReportMessage.uploadId
            var stageName = createReportMessage.stageInfo?.action
            //set report source: AWS, RabbitMQ or Azure Service Bus
            createReportMessage.source = source
            if (stageName.isNullOrEmpty()) {
                stageName = ""
            }
            logger.info("Creating report for uploadId = $uploadId with stageName = $stageName and source = $source")

            ReportManager().createReportWithUploadId(
                createReportMessage.reportSchemaVersion!!,
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
                createReportMessage.source
            )
        } catch (e: BadRequestException) {
            logger.error("createReport - bad request exception: ${e.message}")
        } catch (e: Exception) {
            logger.error("createReport - Failed to process message:${e.message}")
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
    fun sendToDeadLetter(
        invalidData: MutableList<String>,
        validationSchemaFileNames: MutableList<String>,
        createReportMessage: CreateReportMessage
    ) {
        if (invalidData.isNotEmpty()) {
            //This should not run for unit tests
            if (System.getProperty("isTestEnvironment") != "true") {
                ReportManager().createDeadLetterReport(
                    createReportMessage.reportSchemaVersion,
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
                    createReportMessage.source,
                    invalidData,
                    validationSchemaFileNames
                )
            }
            throw BadRequestException(invalidData.joinToString(separator = ","))
        }
    }

}