package gov.cdc.ocio.functions.servicebus

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.exceptions.BadRequestException
import gov.cdc.ocio.exceptions.BadStateException
import gov.cdc.ocio.message.ReportParser
import gov.cdc.ocio.model.*
import gov.cdc.ocio.processingstatusapi.model.reports.ReportNotificationSBMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

/**
 * The service bus is another interface for subscribing for notifications through email.
 *
 * @property context ExecutionContext
 * @constructor
 */
class ReportsNotificationsSBQueueProcessor(private val context: ExecutionContext) {

    private val logger = KotlinLogging.logger {}

    // Use the LONG_OR_DOUBLE number policy, which will prevent Longs from being made into Doubles
    private val gson = GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create()

    /**
     * Process a service bus message with the provided message.
     *
     * @param message String
     * @throws BadStateException
     */
    @Throws(BadStateException::class)
    fun withMessage(message: String) {
        try {
            sendNotificationForReport(gson.fromJson(message, ReportNotificationSBMessage::class.java))
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse CreateReportSBMessage: ${e.localizedMessage}")
            throw BadStateException("Unable to interpret the create report message")
        }
    }

    /**
     * Subscribe for email from the provided service bus message.
     *
     * @param reportNotification ReportNotificationMessage
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    private fun sendNotificationForReport(reportNotification: ReportNotificationSBMessage) {

        val destinationId = reportNotification.destinationId
            ?: throw BadRequestException("Missing required field destination_id")

        val eventType = reportNotification.eventType
            ?: throw BadRequestException("Missing required field event_type")

        val stageName = reportNotification.stageName
            ?: throw BadRequestException("Missing required field stage_name")

        val contentType = reportNotification.contentType
            ?: throw BadRequestException("Missing required field content_type")

        val content: String
        try {
            content = reportNotification.contentAsString
                ?: throw BadRequestException("Missing required field content")
            ReportParser().parseReport(content)
        } catch (ex: BadStateException) {
            // assume a bad request
            throw BadRequestException(ex.localizedMessage)
        }

    }

}