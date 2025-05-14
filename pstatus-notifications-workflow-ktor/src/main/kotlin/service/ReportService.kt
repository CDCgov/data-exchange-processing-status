package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.database.utils.SqlClauseBuilder
import gov.cdc.ocio.processingnotifications.model.UploadInfo
import gov.cdc.ocio.types.model.Status
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit


/**
 * Singleton class for a service layer between the middleware and repository layers.
 *
 * @property repository ProcessingStatusRepository
 * @property cName String
 * @property cVar String
 * @property cPrefix String
 * @property cElFunc String
 * @property timeFunc Long -> String
 */
class ReportService: KoinComponent {
    private val repository by inject<ProcessingStatusRepository>()
    private val cName = repository.reportsCollection.collectionNameForQuery
    private val cVar = repository.reportsCollection.collectionVariable
    private val cPrefix = repository.reportsCollection.collectionVariablePrefix
    private val openBkt = repository.reportsCollection.openBracketChar
    private val closeBkt = repository.reportsCollection.closeBracketChar
    private val cElFunc = repository.reportsCollection.collectionElementForQuery
    private val timeFunc = repository.reportsCollection.timeConversionForQuery

    /**
     * Query the reports collection for number of failed reports of a given action.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param action String
     * @param daysInterval Int?
     */
    fun countFailedReports(
        dataStreamId: String,
        dataStreamRoute: String,
        action: StageAction,
        daysInterval: Int?
    ): Int {
        val query = """
            SELECT VALUE COUNT(1)
            FROM $cName $cVar
            WHERE ${cPrefix}stageInfo.${cElFunc("status")} = '${Status.FAILURE}'
                AND ${cPrefix}stageInfo.${cElFunc("action")} = '$action'
                AND dataStreamId = '$dataStreamId'
                AND dataStreamRoute = '$dataStreamRoute'
                ${appendTimeRange(daysInterval)}
            """.trimIndent()

        return repository.reportsCollection.queryItems(
            query,
            Int::class.java
        ).firstOrNull() ?: 0
    }

    /**
     * Retrieves a list of delayed uploads for a specified data stream and time range.
     * Delayed uploads are defined as uploads that were not completed within a specific time frame.
     *
     * @param dataStreamId The identifier of the data stream being queried.
     * @param dataStreamRoute The route of the data stream being queried.
     * @param daysInterval The number of days to define an additional time range filter. If null, no extra filter is applied.
     * @return A list of delayed uploads, represented by the UploadInfo data class, matching the given criteria.
     */
    fun getDelayedUploads(
        dataStreamId: String,
        dataStreamRoute: String,
        daysInterval: Int?
    ): List<UploadInfo> {
        val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS).epochSecond
        val oneWeekAgo = LocalDate.now().minusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
        val timeRangeSection = """
            AND ${cPrefix}dexIngestDateTime < ${timeFunc(oneHourAgo)} // older than an hour ago
            AND ${cPrefix}dexIngestDateTime > ${timeFunc(oneWeekAgo)} // but not older than a week ago
            ${appendTimeRange(daysInterval)}
        """.trimIndent()
        return getPendingUploads(dataStreamId, dataStreamRoute, timeRangeSection)
    }

    /**
     * Retrieves a list of abandoned uploads for a given data stream. Abandoned uploads are defined as uploads
     * that have an "upload-started" report older than one week but no more than one month, without a corresponding
     * "upload-completed" report in the same time range.
     *
     * @param dataStreamId The identifier of the data stream for which abandoned uploads are being queried.
     * @param dataStreamRoute The route of the data stream for which abandoned uploads are being queried.
     * @return A list of abandoned uploads represented by the UploadInfo data class.
     */
    fun getAbandonedUploads(
        dataStreamId: String,
        dataStreamRoute: String
    ): List<UploadInfo> {
        val oneWeekAgo = LocalDate.now().minusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
        val oneMonthAgo = LocalDate.now().minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
        val timeRangeSection = """
            AND ${cPrefix}dexIngestDateTime < ${timeFunc(oneWeekAgo)}
            AND ${cPrefix}dexIngestDateTime > ${timeFunc(oneMonthAgo)}
        """.trimIndent()
        return getPendingUploads(dataStreamId, dataStreamRoute, timeRangeSection)
    }

    /**
     * Retrieves a list of pending uploads for a specified data stream and time range.
     * Pending uploads are defined as those that have a report indicating the upload has started
     * but no corresponding report indicating completion.
     *
     * @param dataStreamId The identifier of the data stream being queried.
     * @param dataStreamRoute The route of the data stream being queried.
     * @param timeRangeSection A SQL clause string for filtering results based on a specific time range.
     * @return A list of pending uploads, represented by the UploadInfo data class, matching the given criteria.
     */
    private fun getPendingUploads(
        dataStreamId: String,
        dataStreamRoute: String,
        timeRangeSection: String
    ): List<UploadInfo> {
        val query = """
            SELECT ${cPrefix}uploadId,
                mv.content.filename AS filename,
                us.stageInfo.start_processing_time AS uploadStartTime
            FROM $cName $cVar
            LEFT JOIN $cName mv
                ON ${cPrefix}uploadId = mv.uploadId
                AND mv.stageInfo.${cElFunc("action")} = '${StageAction.METADATA_VERIFY}'
            LEFT JOIN $cName us
                ON ${cPrefix}uploadId = us.uploadId
                AND us.stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_STARTED}'
            WHERE ${cPrefix}dataStreamId = '$dataStreamId'
                AND ${cPrefix}dataStreamRoute = '$dataStreamRoute'
                AND ${cPrefix}stageInfo.${cElFunc("action")} IN ${openBkt}'${StageAction.UPLOAD_STARTED}', '${StageAction.UPLOAD_COMPLETED}'${closeBkt}
                $timeRangeSection
            GROUP BY ${cPrefix}uploadId, mv.content.filename, us.stageInfo.start_processing_time
            HAVING
                COUNT(CASE WHEN ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_STARTED}' THEN 1 END) > 0
                AND
                COUNT(CASE WHEN ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_COMPLETED}' THEN 1 END) = 0
            """.trimIndent()

        return repository.reportsCollection.queryItems(
            query,
            UploadInfo::class.java
        )
    }

    /**
     * Query the reports collection for uploads that have completed but have not been delivered for at least an hour.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param daysInterval Int?
     */
    fun getDelayedDeliveries(
        dataStreamId: String,
        dataStreamRoute: String,
        daysInterval: Int?
    ): List<UploadInfo> {
        val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS).epochSecond
        val delayedDeliveriesQuery = """
            SELECT ${cPrefix}uploadId,
                mv.content.filename AS filename,
                us.stageInfo.start_processing_time AS uploadStartTime
            FROM $cName $cVar
            LEFT JOIN $cName mv
                ON ${cPrefix}uploadId = mv.uploadId
                AND mv.stageInfo.${cElFunc("action")} = '${StageAction.METADATA_VERIFY}'
            LEFT JOIN $cName us
                ON ${cPrefix}uploadId = us.uploadId
                AND us.stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_STARTED}'
            WHERE ${cPrefix}dataStreamId = '$dataStreamId'
                AND ${cPrefix}dataStreamRoute = '$dataStreamRoute'
                AND ${cPrefix}stageInfo.${cElFunc("action")} IN ${openBkt}'${StageAction.UPLOAD_COMPLETED}', '${StageAction.FILE_DELIVERY}'${closeBkt}
                AND ${cPrefix}dexIngestDateTime < ${timeFunc(oneHourAgo)}
                ${appendTimeRange(daysInterval)}
            GROUP BY ${cPrefix}uploadId, mv.content.filename, us.stageInfo.start_processing_time
            HAVING
                COUNT(CASE WHEN ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_COMPLETED}' THEN 1 END) > 0
                AND
                COUNT(CASE WHEN ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.FILE_DELIVERY}' THEN 1 END) = 0
        """.trimIndent()

        return repository.reportsCollection.queryItems(
            delayedDeliveriesQuery,
            UploadInfo::class.java
        )
    }

    /**
     * Appends a SQL clause for filtering results based on a time range if a valid interval is provided.
     *
     * @param daysInterval The number of days to define a relative date range. If null, no time range condition is appended.
     * @return A SQL clause string for filtering by the specified time range, or an empty string if no interval is provided.
     */
    private fun appendTimeRange(daysInterval: Int?) = daysInterval?.let {
        "AND ${SqlClauseBuilder.buildSqlClauseForDaysInterval(it, cPrefix, timeFunc)}"
    } ?: ""
}