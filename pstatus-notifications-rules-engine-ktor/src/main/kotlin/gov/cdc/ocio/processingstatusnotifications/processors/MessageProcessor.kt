package gov.cdc.ocio.processingstatusnotifications.processors

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.messagesystem.MessageProcessorInterface
import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import gov.cdc.ocio.processingstatusnotifications.exception.BadStateException
import gov.cdc.ocio.processingstatusnotifications.exception.ContentException
import gov.cdc.ocio.processingstatusnotifications.exception.InvalidSchemaDefException
import gov.cdc.ocio.processingstatusnotifications.model.report.ReportMessage
import gov.cdc.ocio.processingstatusnotifications.model.report.Status
import gov.cdc.ocio.processingstatusnotifications.rulesEngine.RuleEngine
import gov.cdc.ocio.types.adapters.InstantTypeAdapter
import mu.KotlinLogging
import java.time.Instant


/**
 * Processor for handling messages received.
 */
class MessageProcessor: MessageProcessorInterface {

    private val logger = KotlinLogging.logger {}

    // Use the LONG_OR_DOUBLE number policy, which will prevent Longs from being made into Doubles
    private val gson = GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
        .create()

    /**
     * Process the incoming reports, which are already validated.
     *
     * @param message String
     */
    override fun processMessage(message: String) {
        try {
            val report = gson.fromJson(message, ReportMessage::class.java)
            val status = evaluateRulesForReport(report)
            logger.info { "Processed report with resulting status: $status" }
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse CreateReportSBMessage: ${e.localizedMessage}")
            throw BadRequestException("Failed to interpret the report")
        }
    }

    /**
     * Runs the rules engine against the report received.
     *
     * @param report ReportMessage
     * @return Status?
     * @throws BadRequestException
     * @throws InvalidSchemaDefException
     */
    @Throws(BadRequestException::class, InvalidSchemaDefException::class)
    private fun evaluateRulesForReport(
        report: ReportMessage
    ): Status? {

        val dataStreamId = report.dataStreamId
            ?: throw BadRequestException("Missing required field data_stream_id")

        val dataStreamRoute = report.dataStreamRoute
            ?: throw BadRequestException("Missing required field data_stream_route")

        val dispositionType = report.dispositionType
            ?: throw BadRequestException("Missing required field disposition_type")

        val status = report.stageInfo?.status

        return try {
            logger.debug { "Report status: $status" }
            RuleEngine.evaluateAllRules(report)
            status
        } catch (ex: BadStateException) {
            // assume a bad request
            throw BadRequestException(ex.localizedMessage)
        } catch (ex: InvalidSchemaDefException) {
            // assume an invalid request
            throw InvalidSchemaDefException(ex.localizedMessage)
        } catch (ex: ContentException) {
            // assume an invalid request
            throw ContentException(ex.localizedMessage)
        }
    }

}