package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.models.ReportDeadLetter
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.util.*

class ReportDeadLetterLoader : CosmosDeadLetterLoader() {

    fun getByUploadId(uploadId: String): List<ReportDeadLetter> {
        val reportsSqlQuery = "select * from $reportsDeadLetterContainerName r where r.id = '$uploadId'"

        val reportItems = reportsDeadLetterContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )

        val reports = mutableListOf<ReportDeadLetter>()
        reportItems?.forEach { reports.add(daoToReport(it)) }

        return reports
    }

     fun getByDataStreamByDateRange(dataStreamId: String, dataStreamRoute:String, startDate:String, endDate:String): List<ReportDeadLetter> {
         val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
         formatter.timeZone = TimeZone.getTimeZone("UTC") // Set time zone if needed
         val startDateParam: Long = formatter.parse(startDate).toInstant().toEpochMilli()
         val endDateParam : Long = formatter.parse(endDate).toInstant().toEpochMilli()

     val reportsSqlQuery = "select * from $reportsDeadLetterContainerName r where r.dataStreamId = '$dataStreamId' " +
                            "and r.dataStreamRoute= '$dataStreamRoute' and  r.timestamp >=$startDateParam " +
                            "and r.timestamp<=$endDateParam"

     val reportItems = reportsDeadLetterContainer.queryItems(
         reportsSqlQuery, CosmosQueryRequestOptions(),
         ReportDao::class.java
     )

     val reports = mutableListOf<ReportDeadLetter>()
     reportItems?.forEach { reports.add(daoToReport(it)) }

     return reports
 }

    fun getCountByDataStreamByDateRange(dataStreamId: String, dataStreamRoute:String?, startDate:String, endDate:String): Int{
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC") // Set time zone if needed
        val startDateParam: Long = formatter.parse(startDate).toInstant().toEpochMilli()
        val endDateParam : Long = formatter.parse(endDate).toInstant().toEpochMilli()

         val reportsSqlQuery = "select value count(1) from $reportsDeadLetterContainerName r where r.dataStreamId = '$dataStreamId' " +
                "and  r.timestamp >=$startDateParam " +
                "and r.timestamp<=$endDateParam" + if (dataStreamRoute!=null) " and r.dataStreamRoute= '$dataStreamRoute'" else ""

         val reportItems = reportsDeadLetterContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            Int::class.java
        )
        var count = 0
        if (reportItems.iterator().hasNext()) {
             count = reportItems.iterator().next()
            println("Count of records: $count")
        } else {
            println("Count of records: 0")

        }
       return count
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