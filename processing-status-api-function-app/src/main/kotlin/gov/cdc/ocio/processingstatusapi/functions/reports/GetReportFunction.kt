package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.model.Report
import gov.cdc.ocio.processingstatusapi.model.ReportDao
import gov.cdc.ocio.processingstatusapi.model.StageReport
import gov.cdc.ocio.processingstatusapi.model.StageReportSerializer
import java.util.*

/**
 * Collection of ways to get reports.
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property context ExecutionContext
 * @constructor
 */
class GetReportFunction(
        private val request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext) {

    private val logger = context.logger

    private val reportsContainerName = "Reports"
    private val stageReportsContainerName = "StageReports"

    private val reportsContainer by lazy {
        CosmosContainerManager.initDatabaseContainer(context, reportsContainerName, "/uploadId")!!
    }

    private val stageReportsContainer by lazy {
        CosmosContainerManager.initDatabaseContainer(context, stageReportsContainerName, "/uploadId")!!
    }

    private val gson = GsonBuilder()
            .registerTypeAdapter(
                    StageReport::class.java,
                    StageReportSerializer()
            ).create()

    /**
     * Retrieve a report with the provided upload ID.
     *
     * @param uploadId String
     * @return HttpResponseMessage
     */
    fun withUploadId(uploadId: String): HttpResponseMessage {

        // Get the report metadata
        val reportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

        // Locate the existing report so we can amend it
        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            Report::class.java
        )
        if (reportItems.count() > 0) {
            val report = reportItems.elementAt(0)

            val stageReportsSqlQuery = "select * from $stageReportsContainerName r where r.uploadId = '$uploadId'"

            // Locate the existing stage reports
            val stageReportItems = stageReportsContainer.queryItems(
                stageReportsSqlQuery, CosmosQueryRequestOptions(),
                StageReport::class.java
            )
            if (reportItems.count() > 0) {
                val stageReportItemList = stageReportItems.toList()

                logger.info("Successfully located report with uploadId = $uploadId")

                val reportResult = ReportDao().apply {
                    this.uploadId = uploadId
                    this.reportId = report.reportId
                    this.destinationId = report.destinationId
                    this.eventType = report.eventType
                    this.reports = stageReportItemList
                }
                return request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(reportResult))
                    .build()
            }
        }
        logger.warning("Failed to locate report with uploadId = $uploadId")

        return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid uploadId provided")
                .build()
    }

    /**
     * Retrieve a report with the provided report ID.
     *
     * @param reportId String
     * @return HttpResponseMessage
     */
    fun withReportId(reportId: String): HttpResponseMessage {

//        val sqlQuery = "select * from $containerName r where r.reportId = '$reportId'"
//
//        // Locate the existing report so we can amend it
//        val items = container.queryItems(
//                sqlQuery, CosmosQueryRequestOptions(),
//                Report::class.java
//        )
//        if (items.count() > 0) {
//            logger.info("Successfully located report with reportId = $reportId")
//            val report = items.elementAt(0)
//
//            return request
//                    .createResponseBuilder(HttpStatus.OK)
//                    .header("Content-Type", "application/json")
//                    .body(gson.toJson(report))
//                    .build()
//        }
//
//        logger.warning("Failed to locate report with reportId = $reportId")

        return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid reportId provided: $reportId")
                .build()
    }

    fun withDestinationId(destinationId: String, stageName: String): HttpResponseMessage {

//        val eventType = request.queryParameters["eventType"]
//
//        val sqlQuery = StringBuilder()
//        sqlQuery.append("select * from $containerName t where t.destinationId = '$destinationId' and exists (select value u from u in t.reports where u.stageName = '$stageName')")
//
//        eventType?.run {
//            sqlQuery.append(" and t.eventType = '$eventType'")
//        }
//
//        // Locate the existing report so we can amend it
//        val reports = container.queryItems(
//                sqlQuery.toString(), CosmosQueryRequestOptions(),
//                Report::class.java
//        ).toList()
//
//        val reportStages = mutableListOf<StageReport>()
////        reports.forEach { report ->
////            report.reports?.filter { it.stageName == stageName }?.let { reportStages.addAll(it) }
////        }

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                //.body(gson.toJson(reportStages))
                .build()
    }
}