package gov.cdc.ocio.processingstatusapi.functions.reports

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.CreateReportResult
import java.util.*
import java.util.logging.Logger

class CreateReportFunction(private val context: ExecutionContext) {

    private val logger: Logger = context.logger

    fun withHttpRequest(request: HttpRequestMessage<Optional<String>>): HttpResponseMessage {

        logger.info("Processing HTTP triggered request to create a report")

        val uploadId = request.queryParameters["uploadId"]
                ?: return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("uploadId is required")
                        .build()

        val destinationId = request.queryParameters["destinationId"]
                ?: return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("destinationId is required")
                        .build()

        val eventType = request.queryParameters["eventType"]
                ?: return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("eventType is required")
                        .build()

        val reportId = ReportManager(context).createReport(uploadId, destinationId, eventType)

        val result = CreateReportResult().apply {
            this.reportId = reportId
        }

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(result)
                .build()
    }

}