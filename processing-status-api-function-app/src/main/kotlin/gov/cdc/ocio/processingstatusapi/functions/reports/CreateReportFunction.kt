package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.CosmosContainer
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.model.CreateReportResult
import gov.cdc.ocio.processingstatusapi.model.Report
import java.util.*

class CreateReportFunction {

    fun run(
            request: HttpRequestMessage<Optional<String>>,
            context: ExecutionContext
    ): HttpResponseMessage {

        val logger = context.logger

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

        val reportId = UUID.randomUUID().toString()

        // Send report message across service bus
        val report = Report().apply {
            this.id = reportId
            this.reportId = reportId
            this.uploadId = uploadId
            this.destinationId = destinationId
        }

        val container = CosmosContainerManager.initDatabaseContainer(context, "Reports")!!
        createReport(context, container, report)

        val result = CreateReportResult().apply {
            this.reportId = reportId
        }

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(result)
                .build()
    }

    private fun createReport(context: ExecutionContext, container: CosmosContainer, report: Report) {
        val logger = context.logger

        val response = container.createItem(report)

        logger.info("wrote reportId = ${response.item.reportId}")
    }

}