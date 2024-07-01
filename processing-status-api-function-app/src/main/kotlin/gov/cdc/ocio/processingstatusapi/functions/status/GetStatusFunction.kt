package gov.cdc.ocio.processingstatusapi.functions.status

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.model.*
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import gov.cdc.ocio.processingstatusapi.model.reports.ReportDao
import gov.cdc.ocio.processingstatusapi.model.reports.ReportSerializer
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
class GetStatusFunction(
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

    /**
     * Retrieve a complete status (reports) for the provided upload ID.
     *
     * @param uploadId String
     * @return HttpResponseMessage
     */
    fun withUploadId(uploadId: String): HttpResponseMessage {

        val reportResult = getReport(uploadId)

        // Need at least one (report)
        if (reportResult == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid uploadId provided")
                .build()
        }

        val status = StatusResult().apply {
            reports = reportResult?.reports
        }

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(gson.toJson(status))
            .build()
    }

    private fun getReport(uploadId: String): ReportDao? {

        // Get the reports
        val reportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

        // Locate the existing report so we can amend it
        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            Report::class.java
        )
        if (reportItems.count() > 0) {
            val report = reportItems.elementAt(0)
            val stageReportItemList = reportItems.toList()

            logger.info("Successfully located report with uploadId = $uploadId")

            val reportResult = ReportDao().apply {
                this.uploadId = uploadId
                this.dataStreamId = report.dataStreamId
                this.dataStreamRoute = report.dataStreamRoute
                this.reports = stageReportItemList
            }
            return reportResult
        }
        logger.error("Failed to locate report with uploadId = $uploadId")

        return null
    }

}