package gov.cdc.ocio.processingstatusnotifications.servicebus

import com.azure.messaging.servicebus.ServiceBusReceivedMessage
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import gov.cdc.ocio.processingstatusnotifications.exception.BadStateException
import gov.cdc.ocio.processingstatusnotifications.exception.ContentException
import gov.cdc.ocio.processingstatusnotifications.exception.InvalidSchemaDefException
import gov.cdc.ocio.processingstatusnotifications.model.cache.SubscriptionRule
import gov.cdc.ocio.processingstatusnotifications.model.message.ReportNotificationServiceBusMessage
import gov.cdc.ocio.processingstatusnotifications.model.message.SchemaDefinition
import gov.cdc.ocio.processingstatusnotifications.parser.ReportParser
import gov.cdc.ocio.processingstatusnotifications.rulesEngine.RuleEngine

class ReportsNotificationProcessor {

    private val logger = mu.KotlinLogging.logger {}

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
    @Throws(BadRequestException::class, InvalidSchemaDefException::class)
    fun withMessage(message: ServiceBusReceivedMessage): String {
        val sbMessage = message.body.toString()
        try {
            return sendNotificationForReportStatus(gson.fromJson(sbMessage, ReportNotificationServiceBusMessage::class.java))
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse CreateReportSBMessage: ${e.localizedMessage}")
            throw BadRequestException("Unable to interpret the create report message")
        }
    }

    /**
     * Subscribe for notifications from the provided service bus message.
     *
     * @param reportNotification ReportNotificationMessage
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class,InvalidSchemaDefException::class)
    private fun sendNotificationForReportStatus(reportNotification: ReportNotificationServiceBusMessage): String {

        val dataStreamId = reportNotification.dataStreamId
            ?: throw BadRequestException("Missing required field data_stream_id")

        val dataStreamRoute = reportNotification.dataStreamRoute
            ?: throw BadRequestException("Missing required field data_stream_route")

        val stageName = reportNotification.stageName
            ?: throw BadRequestException("Missing required field stage_name")

        val contentType = reportNotification.contentType
            ?: throw BadRequestException("Missing required field content_type")

        val content: String
        val status: String
        try {
            content = reportNotification.contentAsString
                ?: throw BadRequestException("Missing required field content")
            val schemaDef = SchemaDefinition.fromJsonString(content)

            status = ReportParser().parseReportForStatus(content, schemaDef.schemaName)
            logger.debug("Report parsed for status $status")
            RuleEngine.evaluateAllRules(SubscriptionRule(dataStreamId, dataStreamRoute, stageName, status).getStringHash())
            return status.lowercase()
        } catch (ex: BadStateException) {
            // assume a bad request
            throw BadRequestException(ex.localizedMessage)
        } catch(ex: InvalidSchemaDefException) {
            // assume an invalid request
            throw InvalidSchemaDefException(ex.localizedMessage)
        } catch(ex: ContentException) {
            // assume an invalid request
            throw ContentException(ex.localizedMessage)
        }
    }

}