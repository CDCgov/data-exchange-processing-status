package gov.cdc.ocio.functions.servicebus

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.exceptions.BadRequestException
import gov.cdc.ocio.exceptions.BadStateException
import gov.cdc.ocio.exceptions.ContentException
import gov.cdc.ocio.exceptions.InvalidSchemaDefException
import gov.cdc.ocio.message.ReportParser
import gov.cdc.ocio.model.cache.SubscriptionRule
import gov.cdc.ocio.model.message.ReportNotificationSBMessage
import gov.cdc.ocio.model.message.SchemaDefinition
import gov.cdc.ocio.rulesEngine.RuleEngine
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * The service bus is another interface for subscribing for notifications through email.
 *
 * @property context ExecutionContext
 * @constructor
 */
class ReportsNotificationsSBQueueProcessor(private val context: ExecutionContext) {

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
    fun withMessage(message: String): String {
        try {
            return sendNotificationForReportStatus(gson.fromJson(message, ReportNotificationSBMessage::class.java))
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
    private fun sendNotificationForReportStatus(reportNotification: ReportNotificationSBMessage): String {

        val destinationId = reportNotification.destinationId
            ?: throw BadRequestException("Missing required field destination_id")

        val eventType = reportNotification.eventType
            ?: throw BadRequestException("Missing required field event_type")

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
            logger.info("Report parsed for status $status")
            RuleEngine.evaluateAllRules(SubscriptionRule(destinationId, eventType, stageName, status).getStringHash())
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

    /**
     * Process a service bus message with the provided message.
     *
     * @param message String
     * @throws BadStateException
     */
    @Throws(BadRequestException::class, InvalidSchemaDefException::class)
    fun withTestMessageForDispatch(message: String): List<String> {
        try {
            return dispatchEventForReport(gson.fromJson(message, ReportNotificationSBMessage::class.java))
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse CreateReportSBMessage: ${e.localizedMessage}")
            throw BadRequestException("Unable to interpret the create report message")
        }
    }

    /**
     * This method is added purely for testing to see if the events are dispatched when a Report is sent on Service Bus
     * @param reportNotification ReportNotificationSBMessage
     * @return List<String>
     * @throws BadRequestException
     * @throws InvalidSchemaDefException
     */
    @Throws(BadRequestException::class,InvalidSchemaDefException::class)
    private fun dispatchEventForReport(reportNotification: ReportNotificationSBMessage): List<String> {

        val destinationId = reportNotification.destinationId
            ?: throw BadRequestException("Missing required field destination_id")

        val eventType = reportNotification.eventType
            ?: throw BadRequestException("Missing required field event_type")

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
            return RuleEngine.evaluateAllRules(SubscriptionRule(destinationId, eventType, stageName, status).getStringHash())
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