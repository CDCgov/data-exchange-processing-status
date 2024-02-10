package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.InvalidSchemaDefException
import gov.cdc.ocio.processingstatusapi.model.DispositionType
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import gov.cdc.ocio.processingstatusapi.model.reports.stagereports.SchemaDefinition
import gov.cdc.ocio.processingstatusapi.utils.JsonUtils
import mu.KotlinLogging
import java.util.*

/**
 * The report manager interacts directly with CosmosDB to persist and retrieve reports.
 *
 * @property context ExecutionContext
 * @constructor
 */
class ReportManager {

    private val logger = KotlinLogging.logger {}

    private val reportsContainerName = "Reports"
    private val partitionKey = "/uploadId"

    private val reportsContainer by lazy {
        CosmosContainerManager.initDatabaseContainer(reportsContainerName, partitionKey)
    }

    /**
     * Create a report located with the provided upload ID.
     *
     * @param uploadId String
     * @param destinationId String
     * @param eventType String
     * @param stageName String
     * @param contentType String
     * @param content String
     * @param dispositionType DispositionType
     * @return String - stage report identifier
     * @throws BadStateException
     * @throws BadRequestException
     */
    @Throws(BadStateException::class, BadRequestException::class)
    fun createReportWithUploadId(
        uploadId: String,
        destinationId: String,
        eventType: String,
        stageName: String,
        contentType: String,
        content: String,
        dispositionType: DispositionType
    ): String {
        // Verify the content contains the minimum schema information
        try {
            SchemaDefinition.fromJsonString(content)
        } catch(e: InvalidSchemaDefException) {
            throw BadRequestException("Invalid schema definition: ${e.localizedMessage}")
        }

        return createReport(uploadId, destinationId, eventType, stageName, contentType, content, dispositionType)
    }

    /**
     * Create the provided report.  Note the dispositionType indicates whether this will add or replace existing
     * report(s) with this stageName.
     *
     * @param uploadId String
     * @param destinationId String
     * @param eventType String
     * @param stageName String
     * @param contentType String
     * @param content String
     * @param dispositionType DispositionType - indicates whether to add or replace any existing reports for the
     * given stageName.
     * @return String - stage report identifier
     * */
    private fun createReport(uploadId: String,
                             destinationId: String,
                             eventType: String,
                             stageName: String,
                             contentType: String,
                             content: String,
                             dispositionType: DispositionType): String {

        when (dispositionType) {
            DispositionType.REPLACE -> {
                logger.info("Replacing report(s) with stage name = $stageName")
                // Delete all stages matching the report ID with the same stage name
                val sqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId' and r.stageName = '$stageName'"
                val items = reportsContainer?.queryItems(
                    sqlQuery, CosmosQueryRequestOptions(),
                    Report::class.java
                )
                if ((items?.count() ?: 0) > 0) {
                    try {
                        items?.forEach {
                            reportsContainer?.deleteItem(
                                it.id,
                                PartitionKey(it.uploadId),
                                CosmosItemRequestOptions()
                            )
                        }
                        logger.info("Removed all reports with stage name = $stageName")
                    } catch(e: Exception) {
                        throw BadStateException("Issue deleting report: ${e.localizedMessage}")
                    }
                }

                // Now create the new stage report
                return createStageReport(uploadId, destinationId, eventType, stageName, contentType, content)
            }
            DispositionType.ADD -> {
                logger.info("Creating report for stage name = $stageName")
                return createStageReport(uploadId, destinationId, eventType, stageName, contentType, content)
            }
        }
    }

    /**
     * Creates a report for the given stage.
     *
     * @param uploadId String
     * @param destinationId String
     * @param eventType String
     * @param stageName String
     * @param contentType String
     * @param content String
     * @return String
     * @throws BadStateException
     */
    @Throws(BadStateException::class)
    private fun createStageReport(uploadId: String,
                                  destinationId: String,
                                  eventType: String,
                                  stageName: String,
                                  contentType: String,
                                  content: String): String {
        val stageReportId = UUID.randomUUID().toString()
        val stageReport = Report().apply {
            this.id = stageReportId
            this.uploadId = uploadId
            this.reportId = UUID.randomUUID().toString()
            this.destinationId = destinationId
            this.eventType = eventType
            this.stageName = stageName
            this.contentType = contentType
            this.content = if (contentType.lowercase() == "json") JsonUtils.minifyJson(content) else content
        }

        var attempts = 0
        do {
            val response = reportsContainer?.createItem(
                stageReport,
                PartitionKey(uploadId),
                CosmosItemRequestOptions()
            )

            when (response?.statusCode) {
                200 -> {
                    logger.info("Created at ${Date()}, reportId = ${response.item?.reportId}")
                    return stageReportId
                }
                429 -> {
                    // When a 429 occurs, the documentation states the following:
                    // "The collection has exceeded the provisioned throughput limit. Retry the request after the server
                    // specified retry after duration. For more information, see request units."
                    // See: https://learn.microsoft.com/en-us/rest/api/cosmos-db/http-status-codes-for-cosmosdb
                    val recommendedDuration = response.duration
                    Thread.sleep(recommendedDuration.toMillis())
                }
                else -> {
                    // Need to retry regardless
                    Thread.sleep(500)
                }
            }
        } while (attempts++ < 100)

        throw BadStateException("Failed to create reportId = ${stageReport.reportId}")
    }
}