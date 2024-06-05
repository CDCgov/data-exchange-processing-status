package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.models.ReportDeadLetter
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import java.time.ZoneOffset

class ReportDeadLetterLoader : CosmosDeadLetterLoader() {

    fun getByUploadId(uploadId: String): List<ReportDeadLetter> {
        val reportsSqlQuery = "select * from $reportsDeadLetterContainerName r where r.uploadId = '$uploadId'"

        val reportItems = reportsDeadLetterContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )

        val reports = mutableListOf<ReportDeadLetter>()
        reportItems?.forEach { reports.add(daoToReport(it)) }

        return reports
    }
    fun search(ids: List<String>): List<ReportDeadLetter> {
        val quotedIds = ids.joinToString("\",\"", "\"", "\"")

        val reportsSqlQuery = "select * from $reportsDeadLetterContainerName r where r.id in ($quotedIds)"

        val reportItems = reportsDeadLetterContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )

        val reports = mutableListOf<ReportDeadLetter>()
        reportItems?.forEach { reports.add(daoToReport(it)) }

        return reports
    }



    private fun daoToReport(reportDao: ReportDao): ReportDeadLetter {
        return ReportDeadLetter().apply {
            this.id = reportDao.id
            this.uploadId = reportDao.uploadId
            this.reportId = reportDao.reportId
            this.dataStreamId = reportDao.dataStreamId
            this.dataStreamRoute = reportDao.dataStreamRoute
            this.messageId = reportDao.messageId
            this.status = reportDao.status
            this.timestamp = reportDao.timestamp?.toInstant()?.atOffset(ZoneOffset.UTC)
            this.contentType = reportDao.contentType
            this.content = reportDao.contentAsType
        }
    }
}