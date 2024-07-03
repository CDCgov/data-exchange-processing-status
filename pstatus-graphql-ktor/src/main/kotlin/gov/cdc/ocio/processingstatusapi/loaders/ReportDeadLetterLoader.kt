package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.models.ReportDeadLetter
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.utils.SqlClauseBuilder
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.util.*
import mu.KotlinLogging
/**
 * Class for generating reporting queries from cosmos db container which is then wrapped in a graphQl query service
 */
class ReportDeadLetterLoader : CosmosDeadLetterLoader() {

    /**
     * Function that returns a list of DeadLetterReports based on uploadId
     * @param uploadId String
     */
    fun getByUploadId(uploadId: String): List<ReportDeadLetter> {
        val reportsSqlQuery = "select * from $reportsDeadLetterContainerName r where r.id = '$uploadId'"

        val reportItems = reportsDeadLetterContainer?.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )

        val reports = mutableListOf<ReportDeadLetter>()
        reportItems?.forEach { reports.add(daoToReport(it)) }

        return reports
    }

    /**
     * Function which returns list of ReportDeadLetter based on the specified parameters
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param startDate String
     * @param endDate String
     */
     fun getByDataStreamByDateRange(dataStreamId: String, dataStreamRoute:String, startDate:String?, endDate:String?, daysInterval: Int?): List<ReportDeadLetter> {
         val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
         formatter.timeZone = TimeZone.getTimeZone("UTC") // Set time zone if needed
         val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, getFormattedDateAsString(startDate), getFormattedDateAsString(endDate))

        val reportsSqlQuery = "select * from $reportsDeadLetterContainerName r where r.dataStreamId = '$dataStreamId' " +
                "and r.dataStreamRoute= '$dataStreamRoute' " +
                "and  $timeRangeWhereClause"

     val reportItems = reportsDeadLetterContainer?.queryItems(
         reportsSqlQuery, CosmosQueryRequestOptions(),
         ReportDao::class.java
     )

     val reports = mutableListOf<ReportDeadLetter>()
     reportItems?.forEach { reports.add(daoToReport(it)) }

     return reports
 }

    /**
     *  Function which returns count of ReportDeadLetter items based on the specified parameters
     * @param dataStreamId String
     * @param dataStreamRoute String?
     * @param startDate String
     * @param endDate String
     */
    fun getCountByDataStreamByDateRange(dataStreamId: String, dataStreamRoute:String?, startDate:String?, endDate:String?, daysInterval:Int?): Int {
        val logger = KotlinLogging.logger {}
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC") // Set time zone if needed

        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, startDate, endDate)

        val reportsSqlQuery = "select value count(1) from $reportsDeadLetterContainerName r where r.dataStreamId = '$dataStreamId' " +
                "and  $timeRangeWhereClause " + if (dataStreamRoute!=null) " and r.dataStreamRoute= '$dataStreamRoute'" else ""

         val reportItems = reportsDeadLetterContainer?.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            Int::class.java
        )
        val count = reportItems?.count() ?: 0
        logger.info("Count of records: $count")
        return count
    }

    /**
     *
     */
    fun search(ids: List<String>): List<ReportDeadLetter> {
        val quotedIds = ids.joinToString("\",\"", "\"", "\"")

        val reportsSqlQuery = "select * from $reportsDeadLetterContainerName r where r.id in ($quotedIds)"

        val reportItems = reportsDeadLetterContainer?.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )
        val reports = mutableListOf<ReportDeadLetter>()
        reportItems?.forEach { reports.add(daoToReport(it)) }

        return reports
    }

    /**
     * Function which converts cosmos data object to Report obhect
     * @param reportDao ReportDao
     */
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

    /**
     * Function which converts the inputted date to expected date format
     * @param inputDate String
     */
    private fun getFormattedDateAsString(inputDate:String?):String?{
        if(inputDate == null) return null
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val outputDateFormat =  SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
        val date: Date = inputDateFormat.parse(inputDate)
        val outputDateString = outputDateFormat.format(date)
        return outputDateString
    }
}