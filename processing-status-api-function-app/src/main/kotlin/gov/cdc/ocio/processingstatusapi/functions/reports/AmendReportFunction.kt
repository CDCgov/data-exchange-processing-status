package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusMessage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.model.AmendReportRequest
import gov.cdc.ocio.processingstatusapi.model.CreateReportResult
import gov.cdc.ocio.processingstatusapi.model.Report
import gov.cdc.ocio.processingstatusapi.model.StageReport
import java.util.*
import java.util.logging.Logger

class AmendReportFunction(
        private val request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext) {

    private val logger: Logger = context.logger

    private val containerName = "Reports"

    private val container: CosmosContainer

    private val reportStageName: String? = request.queryParameters["stageName"]

    private val requestBody: String? = request.body.orElse("")

    private val amendReportRequest: AmendReportRequest? = try {
        Gson().fromJson(requestBody, AmendReportRequest::class.java)
    } catch (e: JsonSyntaxException) {
        null
    }

    private val reportContentType: String? = amendReportRequest?.contentType

    private val reportContent: String? = amendReportRequest?.content

    init {
        logger.info("reportStageName=$reportStageName, requestBody=$requestBody, reportContentType=$reportContentType, reportContent=$reportContent")
        container = CosmosContainerManager.initDatabaseContainer(context, containerName)!!
    }

    fun withUploadId(uploadId: String): HttpResponseMessage {
        // Verify the request is complete and properly formatted
        checkRequired()?.let { return it }

        val sqlQuery = "select * from $containerName r where r.uploadId = '$uploadId'"

        // Locate the existing report so we can amend it
        val items = container.queryItems(
                sqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
        )
        if (items.count() > 0) {
            logger.info("Successfully located report with uploadId = $uploadId")
            val report = items.elementAt(0)
            return run(report)
        }

        logger.warning("Failed to locate report with uploadId = $uploadId")

        return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid uploadId provided")
                .build()
    }

    fun withReportId(reportId: String): HttpResponseMessage {
        // Verify the request is complete and properly formatted
        checkRequired()?.let { return it }

        val sqlQuery = "select * from $containerName r where r.reportId = '$reportId'"

        // Locate the existing report so we can amend it
        val items = container.queryItems(
                sqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
        )
        if (items.count() > 0) {
            logger.info("Successfully located report with reportId = $reportId")
            val report = items.elementAt(0)
            return run(report)
        }

        logger.warning("Failed to locate report with reportId = $reportId")

        return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid reportId provided")
                .build()
    }

    private fun run(report: Report): HttpResponseMessage {

        amendReport(report)

        val result = CreateReportResult().apply {
            this.reportId = report.reportId
        }

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(result)
                .build()
    }

    private fun amendReport(report: Report) {

        logger.info("Amending reportId = ${report.reportId}")

        val stageReport = StageReport().apply {
            this.reportId = UUID.randomUUID().toString()
            this.stageName = reportStageName
            this.contentType = reportContentType
            this.content = reportContent
        }
        val reports = report.reports?.toMutableList() ?: mutableListOf()
        reports.add(stageReport)
        report.reports = reports

        val response = container.upsertItem(report)
        logger.info("Upserted at ${Date()}, reportId = ${response.item.reportId}")
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

    private fun sendMessage(context: ExecutionContext, message: Report) {
        val logger = context.logger

        //val credential = DefaultAzureCredentialBuilder().build()

        val queueName = System.getenv("ServiceBusQueueName")
        val namespace = System.getenv("ServiceBusNamespace")
        val fqNamespace = "$namespace.servicebus.windows.net"
        val connectionString = System.getenv("ServiceBusConnectionString")

        logger.info("queueName = $queueName")
        logger.info("namespace = $namespace")
        logger.info("Fully qualified service bus namespace = $fqNamespace")

        val senderClient = ServiceBusClientBuilder()
                .fullyQualifiedNamespace(fqNamespace)
                //.credential(credential)
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient()

        // send one message to the queue
        senderClient.sendMessage(ServiceBusMessage(Gson().toJson(message).toString()))
    }
}