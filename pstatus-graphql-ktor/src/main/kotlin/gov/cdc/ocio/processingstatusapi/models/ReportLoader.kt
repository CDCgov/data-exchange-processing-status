package gov.cdc.ocio.processingstatusapi.models

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReportLoader: KoinComponent {

    private val cosmosRepository by inject<CosmosRepository>()

    private val reportsContainerName = "Reports"

    private val reportsContainer = cosmosRepository.reportsContainer

    private val logger = KotlinLogging.logger {}

    fun getByUploadId(uploadId: String): List<Report>? {
        val reportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )

        val reports = mutableListOf<Report>()
        reportItems.forEach { reports.add(daoToReport(it)) }

        return reports
    }

    fun search(ids: List<String>): List<Report> {
        val reportsSqlQuery = "select * from $reportsContainerName r where r.id = '${ids.first()}'"

        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )

        val reports = mutableListOf<Report>()
        reportItems.forEach { reports.add(daoToReport(it)) }

        return reports
    }

    private fun daoToReport(reportDao: ReportDao): Report {
        return Report().apply {
            this.id = reportDao.id
            this.uploadId = reportDao.uploadId
            this.reportId = reportDao.reportId
            this.dataStreamId = reportDao.dataStreamId
            this.dataStreamRoute = reportDao.dataStreamRoute
            this.stageName = reportDao.stageName
            this.messageId = reportDao.messageId
            this.status = reportDao.status
            this.timestamp = reportDao.timestamp
            this.contentType = reportDao.contentType
            this.content = reportDao.contentAsType
        }
    }
}
