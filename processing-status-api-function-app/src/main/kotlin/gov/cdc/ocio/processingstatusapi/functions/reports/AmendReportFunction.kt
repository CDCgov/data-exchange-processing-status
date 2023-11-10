package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosException
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusMessage
import com.google.gson.Gson
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

class AmendReportFunction {

    fun run(
            request: HttpRequestMessage<Optional<String>>,
            uploadId: String,
            context: ExecutionContext
    ): HttpResponseMessage {

        val logger = context.logger

        val stageName = request.queryParameters["stageName"]
                ?: return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("stageName is required")
                        .build()

        val requestBody = request.body.orElse("")
        val amendReportRequest = Gson().fromJson(requestBody, AmendReportRequest::class.java)

        val containerName = "Reports"
        val container = CosmosContainerManager.initDatabaseContainer(context, containerName)!!

        val sqlQuery = StringBuilder()
        sqlQuery.append("select * from $containerName r where r.uploadId = '$uploadId'")

        // Locate the existing report so we can amend it
        val items = container.queryItems(
                sqlQuery.toString(), CosmosQueryRequestOptions(),
                Report::class.java
        )

        var reportId: String? = null
        if (items.count() > 0) {
            val report = items.elementAt(0)
            reportId = report.reportId

            val stageReport = StageReport().apply {
                this.reportId = UUID.randomUUID().toString()
                this.stageName = stageName
                this.contentType = amendReportRequest.contentType
                this.content = amendReportRequest.content
            }
            val reports = report.reports?.toMutableList() ?: mutableListOf()
            reports.add(stageReport)
            report.reports = reports

            val response = container.upsertItem(report)
            logger.info("Upserted at ${Date()}, reportId = ${response.item.reportId}")

            //amendReport(context, container, report)
        }

        val result = CreateReportResult().apply {
            this.reportId = reportId
        }

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(result)
                .build()
    }

    @Throws(Exception::class)
    private fun amendReport(context: ExecutionContext,
                            container: CosmosContainer,
                            reportId: String,
                            destinationId: String,
                            stageReport: StageReport) {

        val logger = context.logger

        logger.info("Amending reportId = $reportId")

        var readReport: Report? = null
        try {
            logger.info("Checking to see if reportId exists...")
            val reportResponse = container.readItem(
                    reportId, PartitionKey(destinationId),
                    Report::class.java
            )
            readReport = reportResponse.item

            logger.info("Found reportId")

            logger.info("Amending report")
            readReport.reports?.toMutableList()?.add(stageReport) ?: run {
                readReport.reports = listOf(stageReport)
            }

        } catch (ex: CosmosException) {
            // If here, the reportId was not found
            throw ex
        }

        logger.info("Calling upsert")
        val response = container.upsertItem(readReport)

        logger.info("Upserted at ${Date()}, reportId = ${response.item.reportId}")
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