package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import java.time.LocalDate


/**
 * Gather the upload durations.
 *
 * @constructor
 */
class UploadDurationQuery(
    repository: ProcessingStatusRepository
): CommonQuery(repository) {

    private fun build(
        utcDateToRun: LocalDate,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>
    ): String {
        val querySB = StringBuilder()

        querySB.append("""
            SELECT RAW ARRAY_AGG(duration) 
            FROM (
                SELECT 
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = 'blob-file-copy' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    - 
                    MIN(CASE WHEN ${cPrefix}stageInfo.action = 'metadata-verify' THEN ${cPrefix}stageInfo.start_processing_time END) 
                    AS duration
                FROM $collectionName $cVar
                WHERE ${cPrefix}stageInfo.action IN ['metadata-verify', 'blob-file-copy']
                """)

        querySB.append(whereClause(utcDateToRun, dataStreamIds, dataStreamRoutes, jurisdictions))

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
            logger.info("Executing upload duration query")
            val results = collection.queryItems(query, Array<Long>::class.java).firstOrNull()
            return@runCatching results?.toList().orEmpty()
        }.getOrElse {
            logger.error("Error occurred while executing query: ${it.localizedMessage}")
            throw it
        }
    }
}