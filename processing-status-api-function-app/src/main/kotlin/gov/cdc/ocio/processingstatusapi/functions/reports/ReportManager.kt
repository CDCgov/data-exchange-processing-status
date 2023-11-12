package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.model.DispositionType
import gov.cdc.ocio.processingstatusapi.model.Report
import gov.cdc.ocio.processingstatusapi.model.StageReport
import java.util.*
import java.util.logging.Logger

class ReportManager(context: ExecutionContext) {

    private val logger: Logger = context.logger

    private val containerName = "Reports"

    private val container: CosmosContainer

    init {
        logger.info("CreateReportFunction")
        container = CosmosContainerManager.initDatabaseContainer(context, containerName)!!
    }

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

        val response = container.createItem(report)
        logger.info("Created report with reportId = ${response.item.reportId}")

        return reportId
    }

    @Throws(BadStateException::class, BadRequestException::class)
    fun amendReportWithUploadId(uploadId: String,
                                stageName: String,
                                contentType: String,
                                content: String,
                                dispositionType: DispositionType): String {
        val sqlQuery = "select * from $containerName r where r.uploadId = '$uploadId'"

        // Locate the existing report so we can amend it
        val items = container.queryItems(
                sqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
        )
        if (items.count() > 0) {
            logger.info("Successfully located report with uploadId = $uploadId")
            val report = items.elementAt(0)

            val reportId = report.reportId ?: throw BadStateException("Unexpected null value for reportId")

            amendReport(report, stageName, contentType, content, dispositionType)

            return reportId
        }

        throw BadRequestException("Unable to locate uploadId: $uploadId")
    }

    @Throws(BadRequestException::class)
    fun amendReportWithReportId(reportId: String,
                                stageName: String,
                                contentType: String,
                                content: String,
                                dispositionType: DispositionType) {
        val sqlQuery = "select * from $containerName r where r.reportId = '$reportId'"

        // Locate the existing report so we can amend it
        val items = container.queryItems(
                sqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
        )
        if (items.count() > 0) {
            logger.info("Successfully located report with reportId = $reportId")
            val report = items.elementAt(0)

            amendReport(report, stageName, contentType, content, dispositionType)

        } else throw BadRequestException("Unable to locate reportId: $reportId")
    }

    private fun amendReport(report: Report,
                            stageName:
                            String,
                            contentType: String,
                            content: String,
                            dispositionType: DispositionType) {

        val reports = report.reports?.toMutableList() ?: mutableListOf()

        when (dispositionType) {
            DispositionType.REPLACE -> {
                // Remove all elements with matching stageName.  The new one will be then added in logic below.
                reports.removeIf { it.stageName == stageName }
            }
            else -> { } // do nothing
        }
        val stageReport = StageReport().apply {
            this.reportId = UUID.randomUUID().toString()
            this.stageName = stageName
            this.contentType = contentType
            this.content = content
        }
        reports.add(stageReport)

        report.reports = reports

        val response = container.upsertItem(report)
        logger.info("Upserted at ${Date()}, reportId = ${response.item.reportId}")
    }
}