package gov.cdc.ocio.processingstatusapi.utils

import gov.cdc.ocio.processingstatusapi.ReportManager
import gov.cdc.ocio.messagesystem.models.ReportMessage
import gov.cdc.ocio.messagesystem.exceptions.BadRequestException
import gov.cdc.ocio.messagesystem.models.Source
import mu.KotlinLogging


/**
 * Utility class for validating reports against predefined schemas. It's intended to be re-used across supported
 * messaging systems: Azure Service Bus, RabbitMQ and AWS SQS to ensure consistent schema validation and error handling.
 */
class SchemaValidation {

    private val logger = KotlinLogging.logger {}

    /**
     * Creates report using ReportManager() and persists to Cosmos DB.
     *
     * @param reportMessage The message that contains details about the report to be processed
     * The message may come from Azure Service Bus, AWS SQS or RabbitMQ.
     * @throws BadRequestException
     * @throws Exception
     */
    fun createReport(reportMessage: ReportMessage, source: Source) {
        try {
            val uploadId = reportMessage.uploadId
            var stageName = reportMessage.stageInfo?.action
            //set report source: AWS, RabbitMQ or Azure Service Bus
            reportMessage.source = source
            if (stageName.isNullOrEmpty()) {
                stageName = ""
            }
            logger.info("Creating report for uploadId = $uploadId with stageName = $stageName and source = $source")

            ReportManager().createReportWithUploadId(
                reportMessage.reportSchemaVersion!!,
                uploadId!!,
                reportMessage.dataStreamId!!,
                reportMessage.dataStreamRoute!!,
                reportMessage.dexIngestDateTime!!,
                reportMessage.messageMetadata,
                reportMessage.stageInfo,
                reportMessage.tags,
                reportMessage.data,
                reportMessage.contentType!!,
                reportMessage.content!!, // it was Content I changed to ContentAsString
                reportMessage.jurisdiction,
                reportMessage.senderId,
                reportMessage.dataProducerId,
                reportMessage.dispositionType,
                reportMessage.source
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
     * @param reportMessage The message that contains details about the report to be processed
     * The message may come from  Azure Service Bus, AWS SQS or RabbitMQ.
     * @throws BadRequestException
     */
    fun sendToDeadLetter(
        source: Source,
        invalidData: MutableList<String>,
        validationSchemaFileNames: MutableList<String>,
        reportMessage: ReportMessage
    ) {
        if (invalidData.isNotEmpty()) {
            //This should not run for unit tests
            if (System.getProperty("isTestEnvironment") != "true") {
                ReportManager().createDeadLetterReport(
                    reportMessage.reportSchemaVersion,
                    reportMessage.uploadId,
                    reportMessage.dataStreamId,
                    reportMessage.dataStreamRoute,
                    reportMessage.dexIngestDateTime,
                    reportMessage.messageMetadata,
                    reportMessage.stageInfo,
                    reportMessage.tags,
                    reportMessage.data,
                    reportMessage.dispositionType,
                    reportMessage.contentType,
                    reportMessage.content,
                    reportMessage.jurisdiction,
                    reportMessage.senderId,
                    reportMessage.dataProducerId,
                    source,
                    invalidData,
                    validationSchemaFileNames
                )
            }
            throw BadRequestException(invalidData.joinToString(separator = ","))
        }
    }

}