package gov.cdc.ocio.processingstatusapi.functions.status

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.model.*
import gov.cdc.ocio.processingstatusapi.model.reports.*
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
        val reportsSqlQuery = "select count(1) as counts, r.stageName from $reportsContainerName r where r.uploadId = '$uploadId' group by r.stageName"

        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportCounts::class.java
        )
        if (reportItems.count() > 0) {

            val firstReportSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId' offset 0 limit 1"

            val firstReportItems = reportsContainer.queryItems(
                firstReportSqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
            )
            val firstReport = firstReportItems.firstOrNull()

            logger.info("Successfully located report with uploadId = $uploadId")

            val reportResult = HL7v2Counts().apply {
                this.uploadId = uploadId
                this.destinationId = firstReport?.destinationId
                this.eventType = firstReport?.eventType
                this.reportCounts = mutableListOf()
                this.reportCounts?.addAll(reportItems)
            }

            return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(gson.toJson(reportResult))
                .build()
        }

        logger.error("Failed to locate report with uploadId = $uploadId")

        return request
            .createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("Invalid uploadId provided")
            .build()
    }

}