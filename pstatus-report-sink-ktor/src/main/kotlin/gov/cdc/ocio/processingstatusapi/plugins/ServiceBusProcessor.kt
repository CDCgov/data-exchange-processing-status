package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusReceivedMessage
import com.azure.messaging.servicebus.models.DeadLetterOptions
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.processingstatusapi.ReportManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.reports.CreateReportSBMessage
import gov.cdc.ocio.processingstatusapi.models.reports.Source
import mu.KotlinLogging
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

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
        var sbMessageId = message.messageId
        var sbMessage = message.body.toString()
        var sbMessageStatus = message.state.name
        try {
            logger.info { "Before Message received = $sbMessage" }
            if (sbMessage.contains("destination_id")) {
                sbMessage = sbMessage.replace("destination_id", "data_stream_id")
            }
            if (sbMessage.contains("event_type")) {
                sbMessage = sbMessage.replace("event_type", "data_stream_route")
            }
            logger.info { "After Message received = $sbMessage" }
            createReport(sbMessageId,sbMessageStatus,gson.fromJson(sbMessage, CreateReportSBMessage::class.java))
        }
        catch (e:IllegalArgumentException){
            println("Validation failed: ${e.message}")
            throw e
        }
        catch (e: JsonSyntaxException) {
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
    private fun createReport(messageId:String, messageStatus:String,createReportMessage: CreateReportSBMessage) {
        try {
            validateReport(createReportMessage)
            val uploadId = createReportMessage.uploadId
            val stageName = createReportMessage.stageName
            logger.info("Creating report for uploadId = ${uploadId} with stageName = $stageName")
            ReportManager().createReportWithUploadId(
                createReportMessage.uploadId!!,
                createReportMessage.dataStreamId!!,
                createReportMessage.dataStreamRoute!!,
                createReportMessage.stageName!!,
                createReportMessage.contentType!!,
                messageId, //createReportMessage.messageId is null
                messageStatus, //createReportMessage.status is null
                createReportMessage.content!!, // it was Content I changed to ContentAsString
                createReportMessage.dispositionType,
                Source.SERVICEBUS
            )
        }
        catch (e:IllegalArgumentException){
           throw e
        }
        catch (e: Exception) {
            println("Failed to process service bus message:${e.message}")

        }

    }

    // Function to validate report
    private fun validateReport(createReportMessage: CreateReportSBMessage) {
        val missingFields = mutableListOf<String>()

        if (createReportMessage.uploadId.isNullOrBlank()) {
            missingFields.add("uploadId")
        }
        if (createReportMessage.dataStreamId.isNullOrBlank()) {
            missingFields.add("dataStreamId")
        }
        if (createReportMessage.dataStreamRoute.isNullOrBlank()) {
            missingFields.add("dataStreamRoute")
        }
        if (createReportMessage.stageName.isNullOrBlank()) {
            missingFields.add("stageName")
        }
        if (createReportMessage.contentType.isNullOrBlank()) {
            missingFields.add("contentType")
        }
        if (isNullOrEmpty(createReportMessage.content)) {
            missingFields.add("content")
        }

        if (missingFields.isNotEmpty()) {
            val reason ="Missing fields: ${missingFields.joinToString(", ")}"

            // Write the content of the deadletter reports to CosmosDb
            ReportManager().createDeadLetterReport(
                createReportMessage.uploadId!!,
                createReportMessage.dataStreamId!!,
                createReportMessage.dataStreamRoute!!,
                createReportMessage.dispositionType,
                createReportMessage.contentType!!,
                createReportMessage.content!!,
                missingFields)

            throw IllegalArgumentException(reason)
        }
    }

    private fun isNullOrEmpty(value: Any?): Boolean {
        return when (value) {
            null -> true
            is String -> value.isEmpty()
            is Collection<*> -> value.isEmpty()
            is Map<*, *> -> value.isEmpty()
            else -> false // You can adjust this to your needs
        }
    }
}