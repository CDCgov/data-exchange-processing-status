package gov.cdc.ocio.processingstatusapi.functions.reports

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.model.*
import gov.cdc.ocio.processingstatusapi.model.reports.CreateReportSBMessage
import gov.cdc.ocio.processingstatusapi.model.reports.CreateReportSBMessageV2
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
        try {
            if(message.contains("destination_id") || message.contains("event_type")){
                createReport(gson.fromJson(message, CreateReportSBMessage::class.java))
            } else if(message.contains("data_stream_id") || message.contains("data_stream_route")){
                createReportV2(gson.fromJson(message, CreateReportSBMessageV2::class.java))
            }
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse CreateReportSBMessage: ${e.localizedMessage}")
            throw BadStateException("Unable to interpret the create report message")
        }
    }

    /**
     * Process a service bus message with the provided message.
     *
     * @param message String
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    fun withMessageV2(message: String) {
        try {
            createReportV2(gson.fromJson(message, CreateReportSBMessageV2::class.java))
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

        val destinationId = createReportMessage.destinationId
            ?: throw BadRequestException("Missing required field destination_id")

        val eventType = createReportMessage.eventType
            ?: throw BadRequestException("Missing required field event_type")

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
            destinationId,
            eventType,
            stageName,
            contentType,
            content,
            createReportMessage.dispositionType,
            Source.SERVICEBUS,
            MetaImplementation.V1
        )
    }

    /**
     * Create a report from the provided service bus message.
     *
     * @param createReportMessage CreateReportSBMessage
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    private fun createReportV2(createReportMessage: CreateReportSBMessageV2) {

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
            Source.SERVICEBUS,
            MetaImplementation.V2
        )
    }
}

enum class MetaImplementation {
    V1,
    V2
}