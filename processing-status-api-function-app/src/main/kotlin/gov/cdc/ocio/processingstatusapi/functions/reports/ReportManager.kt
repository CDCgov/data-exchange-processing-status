package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.InvalidSchemaDefException
import gov.cdc.ocio.processingstatusapi.model.DispositionType
import gov.cdc.ocio.processingstatusapi.model.Report
import gov.cdc.ocio.processingstatusapi.model.StageReport
import gov.cdc.ocio.processingstatusapi.model.stagereports.SchemaDefinition
import gov.cdc.ocio.processingstatusapi.utils.JsonUtils
import java.util.*

/**
 * The report manager interacts directly with CosmosDB to persist and retrieve reports.
 *
 * @property context ExecutionContext
 * @constructor
 */
class ReportManager(context: ExecutionContext) {

    private val logger = context.logger

    private val reportsContainerName = "Reports"
    private val stageReportsContainerName = "StageReports"

    private val reportsContainer by lazy {
        CosmosContainerManager.initDatabaseContainer(context, reportsContainerName, "/uploadId")!!
    }

    private val stageReportsContainer by lazy {
        CosmosContainerManager.initDatabaseContainer(context, stageReportsContainerName, "/uploadId")!!
    }

    init {
        logger.info("CreateReportFunction")
    }

    /**
     * Create a report with the provided information.
     *
     * @param uploadId String
     * @param destinationId String
     * @param eventType String
     * @return String
     */
    fun createReport(uploadId: String, destinationId: String, eventType: String): String {
        val reportId = UUID.randomUUID().toString()

        logger.info("Creating report with reportId = $reportId")

        // Create the report
        val report = Report().apply {
            this.id = reportId
            this.reportId = reportId
            this.uploadId = uploadId
            this.destinationId = destinationId
            this.eventType = eventType
        }

        val response = reportsContainer.createItem(report)
        logger.info("Created report with reportId = ${response.item.reportId}")

        return reportId
    }

    /**
     * Amend an existing report located with the provided upload ID.
     *
     * @param uploadId String
     * @param stageName String
     * @param contentType String
     * @param content String
     * @param dispositionType DispositionType
     * @return String - stage report identifier
     * @throws BadStateException
     * @throws BadRequestException
     */
    @Throws(BadStateException::class, BadRequestException::class)
    fun amendReportWithUploadId(uploadId: String,
                                stageName: String,
                                contentType: String,
                                content: String,
                                dispositionType: DispositionType,
                                crossReferenceReportId: Boolean = true): String {
        // Verify the content contains the minimum schema information
        try {
            SchemaDefinition.fromJsonString(content)
        } catch(e: InvalidSchemaDefException) {
            throw BadRequestException("Invalid schema definition: ${e.localizedMessage}")
        }

        var reportId: String? = null
        if (crossReferenceReportId) {
            val sqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

            // Locate the existing report
            val items = reportsContainer.queryItems(
                sqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
            )
            if (items.count() > 0) {
                logger.info("Successfully located report with uploadId = $uploadId")
                val report = items.elementAt(0)

                reportId = report.reportId ?: throw BadStateException("Unexpected null value for reportId")
            }
        }

        return amendReport(uploadId, reportId, stageName, contentType, content, dispositionType)
    }

    /**
     * Amend an existing report located with the provided report ID.
     *
     * @param reportId String
     * @param stageName String
     * @param contentType String
     * @param content String
     * @param dispositionType DispositionType
     * @return String - stage report identifier
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    fun amendReportWithReportId(reportId: String,
                                stageName: String,
                                contentType: String,
                                content: String,
                                dispositionType: DispositionType): String {
        // Verify the content contains the minimum schema information
        try {
            SchemaDefinition.fromJsonString(content)
        } catch(e: InvalidSchemaDefException) {
            throw BadRequestException("Invalid schema definition: ${e.localizedMessage}")
        }

        val sqlQuery = "select * from $reportsContainerName r where r.reportId = '$reportId'"

        // Locate the existing report so we can amend it
        val items = reportsContainer.queryItems(
            sqlQuery, CosmosQueryRequestOptions(),
            Report::class.java
        )
        if (items.count() > 0) {
            logger.info("Successfully located report with reportId = $reportId")
            val report = items.elementAt(0)

            val uploadId = report.uploadId ?: throw BadStateException("Issue getting uploadId from the report")

            return amendReport(uploadId, reportId, stageName, contentType, content, dispositionType)

        } else throw BadRequestException("Unable to locate reportId: $reportId")
    }

    /**
     * Amend the provided report.  Note the dispositionType indicates whether this amendment will add to or
     * replace any existing reports with this stageName.
     *
     * @param uploadId String
     * @param reportId String?
     * @param stageName String
     * @param contentType String
     * @param content String
     * @param dispositionType DispositionType - indicates whether to add or replace any existing reports for the
     * given stageName.
     * @return String - stage report identifier
     * */
    private fun amendReport(uploadId: String,
                            reportId: String?,
                            stageName: String,
                            contentType: String,
                            content: String,
                            dispositionType: DispositionType): String {

        when (dispositionType) {
            DispositionType.REPLACE -> {
                logger.info("Replacing stage report for stageName = $stageName with reportId = $reportId")
                // Delete all stages matching the report ID with the same stage name
                val sqlQuery = "select * from $stageReportsContainerName r where r.reportId = '$reportId' and r.stageName = '$stageName'"
                val items = stageReportsContainer.queryItems(
                    sqlQuery, CosmosQueryRequestOptions(),
                    StageReport::class.java
                )
                if (items.count() > 0) {
                    try {
                        items.forEach {
                            stageReportsContainer.deleteItem(
                                it.id,
                                PartitionKey(it.uploadId),
                                CosmosItemRequestOptions()
                            )
                        }
                        logger.info("Removed all stages with stage name = $stageName from reportId = $reportId")
                    } catch(e: Exception) {
                        throw BadStateException("Issue deleting report: ${e.localizedMessage}")
                    }
                }

                // Now create the new stage report
                return createStageReport(uploadId, reportId, stageName, contentType, content)
            }
            DispositionType.APPEND -> {
                logger.info("Creating stage report for stageName = $stageName with reportId = $reportId")
                return createStageReport(uploadId, reportId, stageName, contentType, content)
            }
        }
    }

    /**
     * Creates a stage report.
     *
     * @param uploadId String
     * @param reportId String?
     * @param stageName String
     * @param contentType String
     * @param content String
     * @return String - stage report identifier
     */
    private fun createStageReport(uploadId: String,
                                  reportId: String?,
                                  stageName: String,
                                  contentType: String,
                                  content: String): String {
        val stageReportId = UUID.randomUUID().toString()
        val stageReport = StageReport().apply {
            this.id = stageReportId
            this.uploadId = uploadId
            this.reportId = reportId
            this.stageReportId = UUID.randomUUID().toString()
            this.stageName = stageName
            this.contentType = contentType
            this.content = if (contentType.lowercase() == "json") JsonUtils.minifyJson(content) else content
        }

        val response = stageReportsContainer.createItem(
            stageReport,
            PartitionKey(uploadId),
            CosmosItemRequestOptions()
        )
        logger.info("Created at ${Date()}, reportId = ${response.item.reportId}")
        return stageReportId
    }
}