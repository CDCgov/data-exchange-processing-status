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
 * @property oneHourAgo Long
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
    private val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS).epochSecond
    private val oneWeekAgo = LocalDate.now().minusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
    private val oneMonthAgo = LocalDate.now().minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond

    /**
     * Query the reports collection for number of failed reports of a given action.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param action String
     * @param daysInterval Int?
     */
    fun countFailedReports(dataStreamId: String, dataStreamRoute: String, action: StageAction, daysInterval: Int?): Int {
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
     * Query the reports collection for in progress uploads that started at least an hour ago.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param daysInterval Int?
     */
    fun getDelayedUploads(dataStreamId: String, dataStreamRoute: String, daysInterval: Int?): List<UploadInfo> {
        val delayedUploadsQuery = """
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
            WHERE dataStreamId = '$dataStreamId'
                AND dataStreamRoute = '$dataStreamRoute'
                AND ${cPrefix}stageInfo.${cElFunc("action")} IN ${openBkt}'${StageAction.UPLOAD_STARTED}', '${StageAction.UPLOAD_COMPLETED}'${closeBkt}
                AND ${cPrefix}dexIngestDateTime < ${timeFunc(oneHourAgo)}
                ${appendTimeRange(daysInterval)}
            GROUP BY ${cPrefix}uploadId, mv.content.filename, us.stageInfo.start_processing_time
            HAVING
                COUNT(CASE WHEN ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_STARTED}' THEN 1 END) > 0
                AND
                COUNT(CASE WHEN ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_COMPLETED}' THEN 1 END) = 0
            """.trimIndent()

        return repository.reportsCollection.queryItems(
            delayedUploadsQuery,
            UploadInfo::class.java
        )
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
    fun getAbandonedUploads(dataStreamId: String, dataStreamRoute: String): List<UploadInfo> {
        val abandonedUploadsQuery = """
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
                AND ${cPrefix}dexIngestDateTime < ${timeFunc(oneWeekAgo)}
                AND ${cPrefix}dexIngestDateTime > ${timeFunc(oneMonthAgo)}
            GROUP BY ${cPrefix}uploadId, mv.content.filename, us.stageInfo.start_processing_time
            HAVING
                COUNT(CASE WHEN ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_STARTED}' THEN 1 END) > 0
                AND
                COUNT(CASE WHEN ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_COMPLETED}' THEN 1 END) = 0
        """.trimIndent()

        return repository.reportsCollection.queryItems(
            abandonedUploadsQuery,
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