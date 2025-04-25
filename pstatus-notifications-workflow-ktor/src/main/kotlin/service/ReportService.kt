package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.model.UploadInfo
import gov.cdc.ocio.types.model.Status
import io.ktor.server.plugins.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant


/**
 * Singleton class for a service layer between the middleware and repository layers.
 *
 * @property repository ProcessingStatusRepository
 * @property cName String
 * @property cVar String
 * @property cPrefix String
 * @property cElFunc String
 * @property timeFunc Long -> String
 * @property oneHourAgo Long
 */
class ReportService: KoinComponent {
    private val repository by inject<ProcessingStatusRepository>()
    private val cName = repository.reportsCollection.collectionNameForQuery
    private val cVar = repository.reportsCollection.collectionVariable
    private val cPrefix = repository.reportsCollection.collectionVariablePrefix
    private val cElFunc = repository.reportsCollection.collectionElementForQuery
    private val timeFunc = repository.reportsCollection.timeConversionForQuery
    private val oneHourAgo = Instant.now().minusSeconds(3600).epochSecond

    /**
     * Query the reports collection for number of failed reports of a given action.
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param action String
     * @param daysInterval Int?
     */
    fun countFailedReports(dataStreamId: String, dataStreamRoute: String, action: StageAction, daysInterval: Int?): Int {
        val query = "select value count(1) from $cName $cVar " +
                "where ${cPrefix}stageInfo.${cElFunc("status")} = '${Status.FAILURE}' " +
                "and ${cPrefix}stageInfo.${cElFunc("action")} = '$action' " +
                "and dataStreamId = '$dataStreamId' " +
                "and dataStreamRoute = '$dataStreamRoute'"

        return repository.reportsCollection.queryItems(appendTimeRange(query, daysInterval), Int::class.java).firstOrNull() ?: 0
    }

    /**
     * Query the reports collection for in progress uploads that started at least an hour ago.
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param daysInterval Int?
     */
    fun getDelayedUploads(dataStreamId: String, dataStreamRoute: String, daysInterval: Int?): List<String> {
        // first, get uploads that have upload-started reports older than 1 hour
        val uploadsStartedQuery = "select distinct ${cPrefix}uploadId from $cName $cVar " +
                "where dataStreamId = '$dataStreamId' " +
                "and dataStreamRoute = '$dataStreamRoute' " +
                "and ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_STARTED}' " +
                "and ${cPrefix}dexIngestDateTime < ${timeFunc(oneHourAgo)}"
        val uploadsStarted = repository.reportsCollection.queryItems(appendTimeRange(uploadsStartedQuery, daysInterval), UploadInfo::class.java)
            .map { it.uploadId }
            .toSet()

        // then, get uploads that have upload-completed reports older than 1 hour
        val uploadsCompletedQuery = "select distinct ${cPrefix}uploadId from $cName $cVar " +
                "where dataStreamId = '$dataStreamId' " +
                "and dataStreamRoute = '$dataStreamRoute' " +
                "and ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_COMPLETED}' " +
                "and ${cPrefix}dexIngestDateTime < ${timeFunc(oneHourAgo)}"
        val uploadsCompleted = repository.reportsCollection.queryItems(appendTimeRange(uploadsCompletedQuery, daysInterval), UploadInfo::class.java)
            .map { it.uploadId }
            .toSet()

        // then take the difference of those to get uploads that don't have upload-completed
        return (uploadsStarted - uploadsCompleted).toList()
    }

    /**
     * Query the reports collection for uploads that have completed but have not been delivered for at least an hour.
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param daysInterval Int?
     */
    fun getDelayedDeliveries(dataStreamId: String, dataStreamRoute: String, daysInterval: Int?): List<String> {
        // first, get completed uploads older than 1 hour
        val uploadsCompletedQuery = "select distinct ${cPrefix}uploadId from $cName $cVar " +
                "where dataStreamId = '$dataStreamId' " +
                "and dataStreamRoute = '$dataStreamRoute' " +
                "and ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_COMPLETED}' " +
                "and ${cPrefix}dexIngestDateTime < ${timeFunc(oneHourAgo)}"
        val uploadsCompleted = repository.reportsCollection.queryItems(appendTimeRange(uploadsCompletedQuery, daysInterval), UploadInfo::class.java)
            .map { it.uploadId }
            .toSet()

        // then, get uploads that have been successfully delivered
        val uploadsDeliveredQuery = "select distinct ${cPrefix}uploadId from $cName $cVar " +
                "where dataStreamId = '$dataStreamId' " +
                "and dataStreamRoute = '$dataStreamRoute' " +
                "and ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.FILE_DELIVERY}' " +
                "and ${cPrefix}dexIngestDateTime < ${timeFunc(oneHourAgo)}"
        val uploadsDelivered = repository.reportsCollection.queryItems(appendTimeRange(uploadsDeliveredQuery, daysInterval), UploadInfo::class.java)
            .map { it.uploadId }
            .toSet()

        // finally, take the difference to get uploads that haven't been delivered in over an hour
        return (uploadsCompleted - uploadsDelivered).toList()
    }

    /**
     * Helper function for appending the time range to a sql++ query.
     * @param query String
     * @param daysInterval Int?
     */
    private fun appendTimeRange(query: String, daysInterval: Int?): String {
        if (daysInterval != null) {
            return "$query and ${buildSqlClauseForDaysInterval(daysInterval, cPrefix)}"
        }

        return query
    }

    /**
     * Builds an SQL clause to filter records based on a number of days interval.
     *
     * This function constructs a partial SQL query to ensure that records within
     * a certain number of days from the current date (up to the specified interval)
     * are selected. If no interval is provided, an empty clause is returned.
     *
     * @param daysInterval The number of days to define the date range. If null, no filtering is applied.
     * @param cPrefix The prefix to be used for SQL column reference (e.g., table alias).
     * @return A string representing the SQL clause for the date range filter, or an empty string if no interval is provided.
     * @throws NumberFormatException If an error occurs during date parsing.
     * @throws BadRequestException If the provided inputs are deemed invalid.
     */
    @Throws(NumberFormatException::class, BadRequestException::class)
    private fun buildSqlClauseForDaysInterval(daysInterval: Int?, cPrefix: String): String {
        val timeRangeSqlPortion = StringBuilder()
        if (daysInterval != null) {
            val dateStartEpochSecs = DateTime
                .now(DateTimeZone.UTC)
                .minusDays(daysInterval)
                .withTimeAtStartOfDay()
                .toDate()
                .time / 1000
            timeRangeSqlPortion.append("${cPrefix}dexIngestDateTime >= $dateStartEpochSecs")
        }
        return timeRangeSqlPortion.toString()
    }
}