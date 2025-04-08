package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.QueryBuilder
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.model.UploadDigestResponse
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.TimingMetrics
import gov.cdc.ocio.types.InstantRange
import java.time.LocalDate


class UploadTimeDeltaQuery(
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
            SELECT 
                -- Minimum delta
                MIN(delta) AS minDelta,
                
                -- Maximum delta
                MAX(delta) AS maxDelta,
                
                -- Average (mean) delta
                AVG(delta) AS meanDelta,
                
                -- Median delta
                ARRAY_SORT(ARRAY_AGG(delta))[FLOOR(ARRAY_LENGTH(ARRAY_AGG(delta)) / 2)] AS medianDelta
            
            FROM (
                -- Subquery to calculate deltas for each uploadId
                SELECT 
                    r.uploadId,
                    
                    -- Start time from 'upload-started'
                    MIN(CASE WHEN r.stageInfo.action = 'upload-started' THEN r.stageInfo.start_processing_time END) AS start_time,
                    
                    -- End time from 'upload-completed'
                    MAX(CASE WHEN r.stageInfo.action = 'upload-completed' THEN r.stageInfo.end_processing_time END) AS end_time,
                    
                    -- Delta (Processing time difference)
                    MAX(CASE WHEN r.stageInfo.action = 'upload-completed' THEN r.stageInfo.end_processing_time END) 
                    - 
                    MIN(CASE WHEN r.stageInfo.action = 'upload-started' THEN r.stageInfo.start_processing_time END) 
                    AS delta
            
                FROM ProcessingStatus.data.`Reports` r
                WHERE r.stageInfo.action IN ['upload-started', 'upload-completed']
                    AND r.stageInfo.status = 'SUCCESS'
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
                    AND r.dexIngestDateTime >= 0
                    AND r.dexIngestDateTime < 2044070400000
                GROUP BY r.uploadId
            ) AS upload_deltas;
        """)

        return querySB.toString().trimIndent()
    }

    fun run(
        utcDateToRun: LocalDate,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
    ): TimingMetrics {
        return runCatching {
            val query = build(
                utcDateToRun,
                dataStreamIds,
                dataStreamRoutes,
                jurisdictions
            )
            logger.info("Upload time delta metrics query:\n$query")
            val results = collection.queryItems(query, TimingMetrics::class.java).first()
            return@runCatching results
        }.getOrElse {
            logger.error("Error occurred while executing query: ${it.localizedMessage}")
            throw it
        }
    }
}