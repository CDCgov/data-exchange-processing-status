package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.QueryBuilder
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadMetrics
import gov.cdc.ocio.types.InstantRange
import java.time.LocalDate


/**
 * Calculates the time for uploads and various metrics like mean, max, median, etc. as well as file size metrics.
 *
 * @constructor
 */
class UploadMetricsQuery(
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
                ARRAY_SORT(ARRAY_AGG(delta))[FLOOR(ARRAY_LENGTH(ARRAY_AGG(delta)) / 2)] AS medianDelta,
                
                -- Minimum file size
                MIN(file_size) AS minFileSize,
                
                -- Maximum file size
                MAX(file_size) AS maxFileSize,
                
                -- Average (mean) file size
                AVG(file_size) AS meanFileSize,
                
                -- Median file size
                ARRAY_SORT(ARRAY_AGG(file_size))[FLOOR(ARRAY_LENGTH(ARRAY_AGG(file_size)) / 2)] AS medianFileSize
        
            FROM (
                -- Subquery to calculate metrics for each uploadId
                SELECT 
                    ${cPrefix}uploadId,
                    
                    -- Start time from 'upload-started'
                    MIN(CASE WHEN ${cPrefix}stageInfo.action = 'upload-started' THEN ${cPrefix}stageInfo.start_processing_time END) AS start_time,
                    
                    -- End time from 'upload-completed'
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = 'upload-completed' THEN ${cPrefix}stageInfo.end_processing_time END) AS end_time,
                    
                    -- Delta (Processing time difference)
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = 'upload-completed' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    - 
                    MIN(CASE WHEN ${cPrefix}stageInfo.action = 'upload-started' THEN ${cPrefix}stageInfo.start_processing_time END) 
                    AS delta,
                    
                    -- File size
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = 'upload-status' THEN ${cPrefix}content.size END) AS file_size
            
                FROM $collectionName $cVar
                WHERE ${cPrefix}stageInfo.action IN ${openBkt}'upload-started', 'upload-completed', 'upload-status'${closeBkt}
                    AND ${cPrefix}stageInfo.status = 'SUCCESS'
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
                GROUP BY ${cPrefix}uploadId
            ) AS upload_metrics;
        """)

        return querySB.toString().trimIndent()
    }

    fun run(
        utcDateToRun: LocalDate,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
    ): UploadMetrics {
        return runCatching {
            val query = build(
                utcDateToRun,
                dataStreamIds,
                dataStreamRoutes,
                jurisdictions
            )
            logger.info("Upload time delta metrics query:\n$query")
            val results = collection.queryItems(query, UploadMetrics::class.java).first()
            return@runCatching results
        }.getOrElse {
            logger.error("Error occurred while executing query: ${it.localizedMessage}")
            throw it
        }
    }
}