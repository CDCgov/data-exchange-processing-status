package gov.cdc.ocio.processingstatusapi.functions.reports

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.model.*
import gov.cdc.ocio.processingstatusapi.model.reports.CreateReportSBMessage
import gov.cdc.ocio.processingstatusapi.model.reports.Source
import mu.KotlinLogging
import java.util.*

/**
 * The service bus is another interface for receiving reports.
 *
 * @property context ExecutionContext
 * @constructor
 */
class ServiceBusProcessor(private val context: ExecutionContext) {

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
    fun withMessage(message: String) {
        var sbMessage = message
        try {
            logger.info {"Before Message received = $sbMessage" }
            if(sbMessage.contains("destination_id")){
                sbMessage = sbMessage.replace("destination_id", "data_stream_id")
            }
            if(sbMessage.contains("event_type")){
                sbMessage = sbMessage.replace("event_type", "data_stream_route")
            }
            logger.info { "After Message received = $sbMessage" }
            createReport(gson.fromJson(sbMessage, CreateReportSBMessage::class.java))
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse CreateReportSBMessage: ${e.localizedMessage}")
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
    private fun createReport(createReportMessage: CreateReportSBMessage) {

        val uploadId = createReportMessage.uploadId
            ?: throw BadRequestException("Missing required field upload_id")

        val dataStreamId = createReportMessage.dataStreamId
            ?: throw BadRequestException("Missing required field data_stream_id")

        val dataStreamRoute = createReportMessage.dataStreamRoute
            ?: throw BadRequestException("Missing required field data_stream_route")

        val stageName = createReportMessage.stageName
            ?: throw BadRequestException("Missing required field stage_name")

        val contentType = createReportMessage.contentType
            ?: throw BadRequestException("Missing required field content_type")

        val content: String
        try {
            content = createReportMessage.contentAsString
                ?: throw BadRequestException("Missing required field content")
        } catch (ex: BadStateException) {
            // assume a bad request
            throw BadRequestException(ex.localizedMessage)
        }

        logger.info("Creating report for uploadId = $uploadId with stageName = $stageName")
        ReportManager().createReportWithUploadId(
            uploadId,
            dataStreamId,
            dataStreamRoute,
            stageName,
            contentType,
            content,
            createReportMessage.dispositionType,
            Source.SERVICEBUS
        )
    }

}