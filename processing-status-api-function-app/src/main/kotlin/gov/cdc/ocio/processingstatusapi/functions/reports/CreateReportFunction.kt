package gov.cdc.ocio.processingstatusapi.functions.reports

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.model.reports.CreateReportResult
import gov.cdc.ocio.processingstatusapi.model.DispositionType
import mu.KotlinLogging
import java.util.*

/**
 * Amend reports for given request.
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property context ExecutionContext
 * @constructor
 */
class CreateReportFunction(
        private val request: HttpRequestMessage<Optional<String>>,
        private val dispositionType: DispositionType) {

    private val logger = KotlinLogging.logger {}

    private val destinationId = request.queryParameters["destinationId"]

    private val eventType = request.queryParameters["eventType"]

    private val reportStageName = request.queryParameters["stageName"]

    private val requestBody = request.body.orElse("")

    init {
        logger.info("reportStageName=$reportStageName, requestBody=$requestBody, dispositionType=$dispositionType")
    }

    /**
     * Create a report for the given upload ID.  The report content type and content is determined from the
     * request as properties of this class.
     *
     * @param uploadId String
     * @return HttpResponseMessage
     */
    fun jsonWithUploadId(uploadId: String): HttpResponseMessage {
        // Verify the request is complete and properly formatted
        checkRequired()?.let { return it }

        try {
            val stageReportId = ReportManager().createReportWithUploadId(
                uploadId,
                destinationId!!,
                eventType!!,
                reportStageName!!,
                "json",
                requestBody,
                dispositionType
            )
            return successResponse(stageReportId, reportStageName)

        } catch (e: BadRequestException) {
            logger.error("Failed to amend report with uploadId = $uploadId: ${e.localizedMessage}")
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(e.localizedMessage)
                    .build()
        } catch (e: BadStateException) {
            return request
                    .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to amend report: ${e.localizedMessage}")
                    .build()
        }
    }

    /**
     * Helper to provide a successful HTTP response.  Present here for convenient reuse.
     *
     * @param stageReportId String
     * @return HttpResponseMessage
     */
    private fun successResponse(stageReportId: String, stageName: String): HttpResponseMessage {

        val result = CreateReportResult(stageReportId, stageName)

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(result)
                .build()
    }

    /**
     * Checks that all the required query parameters are present in order to process an amendment request.  If not,
     * an appropriate HTTP response message is generated with the details.
     *
     * @return HttpResponseMessage?
     */
    private fun checkRequired(): HttpResponseMessage? {

        if (destinationId == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("destinationId is required")
                .build()
        }

        if (eventType == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("eventType is required")
                .build()
        }

        if (reportStageName == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("stageName is required")
                .build()
        }

        if (requestBody == null) {
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Malformed request body, content is required")
                    .build()
        }

        return null
    }
}