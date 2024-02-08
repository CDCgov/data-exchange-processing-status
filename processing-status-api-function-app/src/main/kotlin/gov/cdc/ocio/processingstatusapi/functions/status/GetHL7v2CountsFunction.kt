package gov.cdc.ocio.processingstatusapi.functions.status

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.model.*
import gov.cdc.ocio.processingstatusapi.model.reports.HL7v2Counts
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import gov.cdc.ocio.processingstatusapi.model.reports.ReportCount
import gov.cdc.ocio.processingstatusapi.model.reports.ReportSerializer
import gov.cdc.ocio.processingstatusapi.model.traces.*
import gov.cdc.ocio.processingstatusapi.utils.JsonUtils
import mu.KotlinLogging
import java.util.*


/**
 * Collection of ways to get reports.
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property context ExecutionContext
 * @constructor
 */
class GetHL7v2CountsFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {

    private val logger = KotlinLogging.logger {}

    private val reportsContainerName = "Reports"
    private val partitionKey = "/uploadId"

    private val reportsContainer by lazy {
        CosmosContainerManager.initDatabaseContainer(reportsContainerName, partitionKey)!!
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            Report::class.java,
            ReportSerializer()
        )
        .registerTypeAdapter(
            Date::class.java,
            JsonUtils.GsonUTCDateAdapter()
        )
        .create()

    fun withUploadId(uploadId: String): HttpResponseMessage {

        // Get the reports
        val reportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

        // Locate the existing report so we can amend it
        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            Report::class.java
        )
        if (reportItems.count() > 0) {
            val report = reportItems.elementAt(0)

            val stageReportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

            // Locate the existing stage reports
            val stageReportItems = reportsContainer.queryItems(
                stageReportsSqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
            )
            if (stageReportItems.count() > 0) {
                val stageReportItemList = stageReportItems.toList()

                logger.info("Successfully located report with uploadId = $uploadId")

                val reportResult = HL7v2Counts().apply {
                    this.uploadId = uploadId
                    this.destinationId = report.destinationId
                    this.eventType = report.eventType
                    val reportCountsMap = mutableMapOf<String, Int>()
                    stageReportItemList.forEach { report ->
                        report.stageName?.let { stageName ->
                            var reportCounts = reportCountsMap[stageName]
                            if (reportCounts == null)
                                reportCounts = 1
                            else
                                reportCounts++
                            reportCountsMap[stageName] = reportCounts
                        }
                    }
                    this.reportCounts = mutableListOf()
                    reportCountsMap.entries.forEach {
                        val reportCount = ReportCount().apply {
                            this.stageName = it.key
                            this.counts = it.value
                        }
                        this.reportCounts?.add(reportCount)
                    }
                }

                return request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(reportResult))
                    .build()
            }
        }

        logger.error("Failed to locate report with uploadId = $uploadId")

        return request
            .createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("Invalid uploadId provided")
            .build()
    }

}