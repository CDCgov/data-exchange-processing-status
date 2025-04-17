package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import java.time.LocalDate


/**
 * Gather the upload durations.
 *
 * @constructor
 */
class UploadDurationQuery private constructor(
    repository: ProcessingStatusRepository,
    dataStreamIds: List<String>,
    dataStreamRoutes: List<String>,
    jurisdictions: List<String>,
    utcDateToRun: LocalDate
): UtcTimeToRunReportQuery("upload duration", repository, dataStreamIds, dataStreamRoutes, jurisdictions, utcDateToRun) {

    class Builder(
        repository: ProcessingStatusRepository
    ): UtcTimeToRunReportQuery.Builder<Builder>(repository) {
        override fun build() = UploadDurationQuery(
            repository, dataStreamIds, dataStreamRoutes, jurisdictions, utcDateToRun
        )
    }

    override fun buildSql(): String {
        val querySB = StringBuilder()

        querySB.append("""
            SELECT RAW ARRAY_AGG(duration) 
            FROM (
                SELECT 
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.FILE_DELIVERY}' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    - 
                    MIN(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.METADATA_VERIFY}' THEN ${cPrefix}stageInfo.start_processing_time END) 
                    AS duration
                FROM $collectionName $cVar
                WHERE ${cPrefix}stageInfo.action IN ['${StageAction.METADATA_VERIFY}', '${StageAction.FILE_DELIVERY}']
                """)

        querySB.append(whereClause(utcDateToRun))

        querySB.append("""
                GROUP BY ${cPrefix}uploadId
            ) subquery
            WHERE duration IS NOT NULL;
        """)

        return querySB.toString().trimIndent()
    }

    /**
     * Will return a list of longs, where each long is a duration in seconds.
     *
     * @return List<Long>
     */
    fun run(): List<Long> {
        val results = runQuery(Array<Long>::class.java).firstOrNull()
        return results?.toList().orEmpty()
    }
}