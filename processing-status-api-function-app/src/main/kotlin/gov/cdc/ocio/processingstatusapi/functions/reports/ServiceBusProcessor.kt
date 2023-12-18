package gov.cdc.ocio.processingstatusapi.functions.reports

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.model.*
import gov.cdc.ocio.processingstatusapi.model.reports.CreateReportSBMessage
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

    /**
     * Process a service bus message with the provided message.
     *
     * @param message String
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    fun withMessage(message: String) {
        try {
            createReport(Gson().fromJson(message, CreateReportSBMessage::class.java))
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
            ?: throw BadRequestException("Missing required field uploadId")

        val destinationId = createReportMessage.destinationId
            ?: throw BadRequestException("Missing required field destinationId")

        val eventType = createReportMessage.eventType
            ?: throw BadRequestException("Missing required field eventType")

        val stageName = createReportMessage.stageName
            ?: throw BadRequestException("Missing required field stageName")

        val contentType = createReportMessage.contentType
            ?: throw BadRequestException("Missing required field contentType")

        val content = createReportMessage.content
            ?: throw BadRequestException("Missing required field content")

        logger.info("Creating report for uploadId = $uploadId with stageName = $stageName")
        ReportManager().createReportWithUploadId(
            uploadId,
            destinationId,
            eventType,
            stageName,
            contentType,
            content,
            createReportMessage.dispositionType
        )
    }

}