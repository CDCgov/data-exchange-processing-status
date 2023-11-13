package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.google.gson.Gson
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.model.Report
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

    private val container: CosmosContainer

    init {
        container = CosmosContainerManager.initDatabaseContainer(context, containerName)!!
    }

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
                    .body(Gson().toJson(report).toString())
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
                    .body(Gson().toJson(report).toString())
                    .build()
        }

        logger.warning("Failed to locate report with reportId = $reportId")

        return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid reportId provided")
                .build()
    }

}