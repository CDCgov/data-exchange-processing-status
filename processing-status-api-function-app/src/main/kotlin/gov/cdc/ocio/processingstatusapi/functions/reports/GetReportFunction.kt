package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.model.Report
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

    private val containerName = "Reports"

    private val container = CosmosContainerManager.initDatabaseContainer(context, containerName)!!

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

        val sqlQuery = "select * from $containerName r where r.uploadId = '$uploadId'"

        // Locate the existing report so we can amend it
        val items = container.queryItems(
                sqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
        )
        if (items.count() > 0) {
            logger.info("Successfully located report with uploadId = $uploadId")
            val report = items.elementAt(0)

            return request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(report))
                    .build()
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

        val sqlQuery = "select * from $containerName r where r.reportId = '$reportId'"

        // Locate the existing report so we can amend it
        val items = container.queryItems(
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

        logger.warning("Failed to locate report with reportId = $reportId")

        return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid reportId provided")
                .build()
    }

    fun withDestinationId(destinationId: String, stageName: String): HttpResponseMessage {

        val sqlQuery = "select * from $containerName t where t.destinationId = '$destinationId' and exists (select value u from u in t.reports where u.stageName = '$stageName')"

        // Locate the existing report so we can amend it
        val reports = container.queryItems(
                sqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
        ).toList()

        val reportStages = mutableListOf<StageReport>()
        reports.forEach { report ->
            report.reports?.filter { it.stageName == stageName }?.let { reportStages.addAll(it) }
        }

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(gson.toJson(reportStages))
                .build()
    }
}