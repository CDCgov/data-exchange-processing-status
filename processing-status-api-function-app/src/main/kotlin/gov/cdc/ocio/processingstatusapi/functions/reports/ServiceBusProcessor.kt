package gov.cdc.ocio.processingstatusapi.functions.reports

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.model.*
import java.util.*
import java.util.logging.Logger

class ServiceBusProcessor(private val context: ExecutionContext) {

    private val logger: Logger = context.logger

    @Throws(BadRequestException::class)
    fun withMessage(message: String) {
        val serviceBusMessage = try {
            Gson().fromJson(message, ServiceBusMessage::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }

        if (serviceBusMessage != null) {
            when (serviceBusMessage.requestType) {
                RequestType.CREATE -> {
                    try {
                        createReport(Gson().fromJson(message, CreateReportSBMessage::class.java))
                    } catch (e: JsonSyntaxException) {
                        throw BadStateException("Unable to interpret the create report message")
                    }
                }
                RequestType.AMEND -> {
                    try {
                        amendReport(Gson().fromJson(message, AmendReportSBMessage::class.java))
                    } catch (e: JsonSyntaxException) {
                        throw BadStateException("Unable to interpret the amend report message")
                    }
                }
                else -> throw BadRequestException("Invalid request type")
            }
        }
    }

    @Throws(BadRequestException::class)
    private fun createReport(createReportMessage: CreateReportSBMessage) {

        val uploadId = createReportMessage.uploadId
                ?: throw BadRequestException("Missing required field uploadId")

        val destinationId = createReportMessage.destinationId
                ?: throw BadRequestException("Missing required field destinationId")

        val eventType = createReportMessage.eventType
                ?: throw BadRequestException("Missing required field eventType")

        ReportManager(context).createReport(
                uploadId,
                destinationId,
                eventType
        )
    }

    @Throws(BadRequestException::class)
    private fun amendReport(amendReportMessage: AmendReportSBMessage) {

        val uploadId = amendReportMessage.uploadId
                ?: throw BadRequestException("Missing required field uploadId")

        val stageName = amendReportMessage.stageName
                ?: throw BadRequestException("Missing required field stageName")

        val contentType = amendReportMessage.contentType
                ?: throw BadRequestException("Missing required field contentType")

        val content = amendReportMessage.content
                ?: throw BadRequestException("Missing required field content")

        ReportManager(context).amendReportWithUploadId(
                uploadId,
                stageName,
                contentType,
                content,
                amendReportMessage.dispositionType
        )
    }

}