package gov.cdc.ocio.functions.servicebus

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.exceptions.BadRequestException
import gov.cdc.ocio.exceptions.BadStateException
import gov.cdc.ocio.model.*
import gov.cdc.ocio.model.message.SubscriptionSBMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

/**
 * The service bus is another interface for subscribing for notifications through email.
 *
 * @property context ExecutionContext
 * @constructor
 */
class EmailServiceBusProcessor(private val context: ExecutionContext) {

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
            subscribeForEmailNotifications(Gson().fromJson(message, SubscriptionSBMessage::class.java))
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse CreateReportSBMessage: ${e.localizedMessage}")
            throw BadStateException("Unable to interpret the create report message")
        }
    }

    /**
     * Subscribe for email from the provided service bus message.
     *
     * @param subscriptionMessage EmailSubscriptionMessage
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    private fun subscribeForEmailNotifications(subscriptionMessage: SubscriptionSBMessage) {

        val destinationId = subscriptionMessage.destinationId
            ?: throw BadRequestException("Missing required field destination_id")

        val eventType = subscriptionMessage.eventType
            ?: throw BadRequestException("Missing required field event_type")

        val source = subscriptionMessage.source
            ?: throw BadRequestException("Missing required field email")

        val stageName = subscriptionMessage.stageName
            ?: throw BadRequestException("Missing required field stage_name")

        val statusType = subscriptionMessage.statusType
            ?: throw BadRequestException("Missing required field status_type")

        val contentType = subscriptionMessage.contentType
            ?: throw BadRequestException("Missing required field content_type")

        val content: String
        try {
            content = subscriptionMessage.contentAsString
                ?: throw BadRequestException("Missing required field content")
        } catch (ex: BadStateException) {
            // assume a bad request
            throw BadRequestException(ex.localizedMessage)
        }

        logger.info("Subscribing email = $source for $statusType on Destination " +
                "$destinationId for eventType $eventType and stageName = $stageName")
        SubscriptionManager().subscribeForEmail(
            destinationId,
            eventType,
            source,
            stageName,
            statusType,
            content,
            contentType
        )
    }

}