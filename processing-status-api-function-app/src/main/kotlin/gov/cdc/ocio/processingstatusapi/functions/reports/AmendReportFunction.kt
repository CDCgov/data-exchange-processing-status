package gov.cdc.ocio.processingstatusapi.functions.reports

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.model.AmendReportRequest
import gov.cdc.ocio.processingstatusapi.model.CreateReportResult
import gov.cdc.ocio.processingstatusapi.model.DispositionType
import java.util.*

class AmendReportFunction(
        private val request: HttpRequestMessage<Optional<String>>,
        private val context: ExecutionContext) {

    private val logger = context.logger

    private val reportStageName = request.queryParameters["stageName"]

    private val requestBody = request.body.orElse("")

    private val amendReportRequest = try {
        Gson().fromJson(requestBody, AmendReportRequest::class.java)
    } catch (e: JsonSyntaxException) {
        null
    }

    private val reportContentType = amendReportRequest?.contentType

    private val reportContent = amendReportRequest?.content

    private val dispositionType = amendReportRequest?.dispositionType ?: DispositionType.APPEND

    init {
        logger.info("reportStageName=$reportStageName, requestBody=$requestBody, reportContentType=$reportContentType, reportContent=$reportContent")
    }

    fun withUploadId(uploadId: String): HttpResponseMessage {
        // Verify the request is complete and properly formatted
        checkRequired()?.let { return it }

        try {
            val reportId = ReportManager(context).amendReportWithUploadId(
                    uploadId,
                    reportStageName!!,
                    reportContentType!!,
                    reportContent!!,
                    dispositionType
            )
            return successResponse(reportId)

        } catch (e: BadRequestException) {
            logger.warning("Failed to locate report with uploadId = $uploadId")
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid uploadId provided")
                    .build()
        } catch (e: BadStateException) {
            return request
                    .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to amend report: ${e.localizedMessage}")
                    .build()
        }
    }

    fun withReportId(reportId: String): HttpResponseMessage {
        // Verify the request is complete and properly formatted
        checkRequired()?.let { return it }

        try {
            ReportManager(context).amendReportWithReportId(
                    reportId,
                    reportStageName!!,
                    reportContentType!!,
                    reportContent!!,
                    dispositionType
            )
            return successResponse(reportId)

        } catch (e: BadRequestException) {
            logger.warning("Failed to locate report with reportId = $reportId")
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid reportId provided")
                    .build()
        }
    }

    private fun successResponse(reportId: String): HttpResponseMessage {

        val result = CreateReportResult().apply {
            this.reportId = reportId
        }

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(result)
                .build()
    }

    private fun checkRequired(): HttpResponseMessage? {

        if (reportStageName == null) {
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("stageName is required")
                    .build()
        }

        if (amendReportRequest == null) {
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Malformed request body")
                    .build()
        }

        if (reportContentType == null) {
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Malformed request body, contentType is required")
                    .build()
        }

        if (reportContent == null) {
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Malformed request body, content is required")
                    .build()
        }

        return null
    }
}