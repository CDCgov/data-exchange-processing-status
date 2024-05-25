package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import java.time.ZoneOffset

class ReportLoader: CosmosLoader() {

    fun getAnyReport(): Report? {
        val reportsSqlQuery = "select * from $reportsContainerName r offset 0 limit 1"
        val reportItems = reportsContainer?.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )

        return if (reportItems == null || reportItems.count() == 0)
            null
        else {
            daoToReport(reportItems.first())
        }
    }

    fun getByUploadId(uploadId: String): List<Report> {
        val reportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

        val reportItems = reportsContainer?.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )

        val reports = mutableListOf<Report>()
        reportItems?.forEach { reports.add(daoToReport(it)) }

        return reports
    }

    fun search(ids: List<String>): List<Report> {
        val reportsSqlQuery = "select * from $reportsContainerName r where r.id = '${ids.first()}'"

        val reportItems = reportsContainer?.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )

        val reports = mutableListOf<Report>()
        reportItems?.forEach { reports.add(daoToReport(it)) }

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
            this.timestamp = reportDao.timestamp?.toInstant()?.atOffset(ZoneOffset.UTC)
            this.contentType = reportDao.contentType
            this.content = reportDao.contentAsType
        }
    }
}
