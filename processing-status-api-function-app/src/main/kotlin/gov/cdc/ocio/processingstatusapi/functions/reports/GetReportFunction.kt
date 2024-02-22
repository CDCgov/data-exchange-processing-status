package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.model.*
import gov.cdc.ocio.processingstatusapi.model.reports.*
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
class GetReportFunction(
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
     * Retrieve a report with the provided upload ID.
     *
     * @param uploadId String
     * @return HttpResponseMessage
     */
    fun withUploadId(uploadId: String, version: MetaImplementation): HttpResponseMessage {

        // Get the report metadata
        val reportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

        if(version.equals(MetaImplementation.V2)){
            // Locate the existing report so we can amend it
            val reportItems = reportsContainer.queryItems(
                reportsSqlQuery, CosmosQueryRequestOptions(),
                ReportV2::class.java
            )
            if (reportItems.count() > 0) {
                val report = reportItems.elementAt(0)

                val stageReportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

                // Locate the existing stage reports
                val stageReportItems = reportsContainer.queryItems(
                    stageReportsSqlQuery, CosmosQueryRequestOptions(),
                    ReportV2::class.java
                )
                if (stageReportItems.count() > 0) {
                    val stageReportItemList = stageReportItems.toList()

                    logger.info("Successfully located report with uploadId = $uploadId")

                    val reportResult = ReportDaoV2().apply {
                        this.uploadId = uploadId
                        this.dataStreamId = report.dataStreamId
                        this.dataStreamRoute = report.dataStreamRoute
                        this.reports = stageReportItemList
                    }
                    return request
                        .createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(gson.toJson(reportResult))
                        .build()
                }
            }
        } else {
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

                    val reportResult = ReportDao().apply {
                        this.uploadId = uploadId
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
        }



        logger.error("Failed to locate report with uploadId = $uploadId")

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
    fun withReportId(reportId: String, version: MetaImplementation): HttpResponseMessage {

        val sqlQuery = "select * from $reportsContainerName r where r.reportId = '$reportId'"

        if(version.equals(MetaImplementation.V2)){
            // Locate the existing report so we can amend it
            val items = reportsContainer.queryItems(
                sqlQuery, CosmosQueryRequestOptions(),
                ReportV2::class.java
            )
            if (items.count() > 0) {
                logger.info("Successfully located report with reportId = $reportId")
                val report = items.elementAt(0)

                return request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(report))
                    .build()
            }
        } else{
            // Locate the existing report so we can amend it
            val items = reportsContainer.queryItems(
                sqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
            )
            if (items.count() > 0) {
                logger.info("Successfully located report with reportId = $reportId")
                val report = items.elementAt(0)

                return request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(report))
                    .build()
            }
        }
         logger.error("Failed to locate report with reportId = $reportId")

        return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid reportId provided: $reportId")
                .build()
    }

    /**
     * Retrieve reports for the provided destination ID and stage name.
     *
     * @param destinationId String
     * @param stageName String
     * @return HttpResponseMessage
     */
    fun withDestinationId(dataStreamId: String, stageName: String, version: MetaImplementation): HttpResponseMessage {
        if(version == MetaImplementation.V2) {
            val dataStreamRoute = request.queryParameters["dataStreamRoute"]

            val sqlQuery = StringBuilder()
            sqlQuery.append("select * from $reportsContainerName t where t.dataStreamId = '$dataStreamId' and t.stageName = '$stageName'")

            dataStreamRoute?.run {
                sqlQuery.append(" and t.dataStreamRoute = '$dataStreamRoute'")
            }

            // Locate the existing report so we can amend it
            val reports = reportsContainer.queryItems(
                sqlQuery.toString(), CosmosQueryRequestOptions(),
                ReportV2::class.java
            ).toList()

            return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(gson.toJson(reports))
                .build()
        } else {
            val eventType = request.queryParameters["eventType"]

            val sqlQuery = StringBuilder()
            sqlQuery.append("select * from $reportsContainerName t where t.destinationId = '$dataStreamId' and t.stageName = '$stageName'")

            eventType?.run {
                sqlQuery.append(" and t.eventType = '$eventType'")
            }

            // Locate the existing report so we can amend it
            val reports = reportsContainer.queryItems(
                sqlQuery.toString(), CosmosQueryRequestOptions(),
                Report::class.java
            ).toList()

            return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(gson.toJson(reports))
                .build()
        }

    }
}