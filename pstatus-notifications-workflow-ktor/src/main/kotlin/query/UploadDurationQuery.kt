package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.QueryBuilder
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.types.InstantRange
import java.time.LocalDate


/**
 * Gather the upload durations.
 *
 * @constructor
 */
class UploadDurationQuery(
    repository: ProcessingStatusRepository
): QueryBuilder(repository.reportsCollection) {

    private fun build(
        utcDateToRun: LocalDate,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>
    ): String {
        val instantRange = InstantRange.fromLocalDate(utcDateToRun)
        val startTime = timeFunc(instantRange.start.epochSecond)
        val endTime = timeFunc(instantRange.endInclusive.epochSecond)

        val jurisdictionIdsList = listForQuery(jurisdictions)
        val dataStreamIdsList = listForQuery(dataStreamIds)
        val dataStreamRoutesList = listForQuery(dataStreamRoutes)

        val querySB = StringBuilder()

        querySB.append("""
            SELECT RAW ARRAY_AGG(duration) 
            FROM (
                SELECT 
                    MAX(CASE WHEN r.stageInfo.action = 'blob-file-copy' THEN r.stageInfo.end_processing_time END) 
                    - 
                    MIN(CASE WHEN r.stageInfo.action = 'metadata-verify' THEN r.stageInfo.start_processing_time END) 
                    AS duration
                FROM ProcessingStatus.data.`Reports` r
                WHERE r.stageInfo.action IN ['metadata-verify', 'blob-file-copy']
                """)

        if (dataStreamIds.isNotEmpty())
            querySB.append("""
                    AND ${cPrefix}dataStreamId IN ${openBkt}$dataStreamIdsList${closeBkt}
            """)

        if (dataStreamRoutesList.isNotEmpty())
            querySB.append("""
                    AND ${cPrefix}dataStreamRoute IN ${openBkt}$dataStreamRoutesList${closeBkt}
            """)

        if (jurisdictionIdsList.isNotEmpty())
            querySB.append("""
                    AND ${cPrefix}jurisdiction IN ${openBkt}$jurisdictionIdsList${closeBkt}
            """)

        querySB.append("""
                    AND ${cPrefix}dexIngestDateTime >= $startTime
                    AND ${cPrefix}dexIngestDateTime < $endTime
        """)

        querySB.append("""
                GROUP BY r.uploadId
            ) subquery
            WHERE duration IS NOT NULL;
        """)

        return querySB.toString().trimIndent()
    }

    /**
     * Will return a list of longs, where each long is a duration in seconds.
     *
     * @param utcDateToRun LocalDate
     * @param dataStreamIds List<String>
     * @param dataStreamRoutes List<String>
     * @param jurisdictions List<String>
     * @return List<Long>
     */
    fun run(
        utcDateToRun: LocalDate,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
    ): List<Long> {
        return runCatching {
            val query = build(
                utcDateToRun,
                dataStreamIds,
                dataStreamRoutes,
                jurisdictions
            )
            logger.info("Upload duration query:\n$query")
            val results = collection.queryItems(query, Array<Long>::class.java).first()
            return@runCatching results.toList()
        }.getOrElse {
            logger.error("Error occurred while executing query: ${it.localizedMessage}")
            throw it
        }
    }
}