package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadMetrics
import java.time.LocalDate


/**
 * Calculates the time for uploads and various metrics like mean, max, median, etc. as well as file size metrics.
 *
 * @constructor
 */
class UploadMetricsQuery(
    repository: ProcessingStatusRepository
): ReportQuery(repository) {

    private fun build(
        utcDateToRun: LocalDate,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>
    ): String {
        val querySB = StringBuilder()

        querySB.append("""
            SELECT 
                -- Upload minimum delta
                MIN(upload_delta) AS minUploadDeltaInMillis,
                
                -- Upload maximum delta
                MAX(upload_delta) AS maxUploadDeltaInMillis,
                
                -- Upload average (mean) delta
                AVG(upload_delta) AS meanUploadDeltaInMillis,
                
                -- Upload median delta
                ARRAY_SORT(ARRAY_AGG(upload_delta))[FLOOR(ARRAY_LENGTH(ARRAY_AGG(upload_delta)) / 2)] AS medianUploadDeltaInMillis,
                
                -- Delivery minimum delta
                MIN(delivery_delta) AS minDeliveryDeltaInMillis,
                
                -- Delivery maximum delta
                MAX(delivery_delta) AS maxDeliveryDeltaInMillis,
                
                -- Delivery average (mean) delta
                AVG(delivery_delta) AS meanDeliveryDeltaInMillis,
                
                -- Median delta
                ARRAY_SORT(ARRAY_AGG(delivery_delta))[FLOOR(ARRAY_LENGTH(ARRAY_AGG(delivery_delta)) / 2)] AS medianDeliveryDeltaInMillis,
                
                -- Total duration minimum delta
                MIN(total_delta) AS minTotalDeltaInMillis,
                
                -- Total duration maximum delta
                MAX(total_delta) AS maxTotalDeltaInMillis,
                
                -- Total duration average (mean) delta
                AVG(total_delta) AS meanTotalDeltaInMillis,
                
                -- Total duration median delta
                ARRAY_SORT(ARRAY_AGG(total_delta))[FLOOR(ARRAY_LENGTH(ARRAY_AGG(total_delta)) / 2)] AS medianTotalDeltaInMillis,
                
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
                    
                    -- Upload time delta
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = 'upload-completed' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    - 
                    MIN(CASE WHEN ${cPrefix}stageInfo.action = 'metadata-verify' THEN ${cPrefix}stageInfo.start_processing_time END) 
                    AS upload_delta,
                    
                    -- Delivery time including latency delta (end of the copy minus the end of the upload completion times)
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = 'blob-file-copy' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    - 
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = 'upload-completed' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    AS delivery_delta,
                    
                    -- Total time delta (end to end duration)
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = 'blob-file-copy' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    - 
                    MIN(CASE WHEN ${cPrefix}stageInfo.action = 'metadata-verify' THEN ${cPrefix}stageInfo.start_processing_time END) 
                    AS total_delta,
                    
                    -- File size
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = 'upload-status' THEN ${cPrefix}content.size END) AS file_size
            
                FROM $collectionName $cVar
                WHERE ${cPrefix}stageInfo.action IN ${openBkt}'metadata-verify', 'upload-completed', 'upload-status', 'blob-file-copy'${closeBkt}
            """)

        querySB.append(whereClause(utcDateToRun, dataStreamIds, dataStreamRoutes, jurisdictions))

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
            logger.info("Executing upload time delta metrics query")
            val results = collection.queryItems(query, UploadMetrics::class.java).first()
            return@runCatching results
        }.getOrElse {
            logger.error("Error occurred while executing query: ${it.localizedMessage}")
            throw it
        }
    }
}