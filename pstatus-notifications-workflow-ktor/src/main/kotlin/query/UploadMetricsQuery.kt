package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadMetrics
import java.time.LocalDate


/**
 * Calculates the time for uploads and various metrics like mean, max, median, etc. as well as file size metrics.
 *
 * @constructor
 */
class UploadMetricsQuery private constructor(
    repository: ProcessingStatusRepository,
    dataStreamIds: List<String>,
    dataStreamRoutes: List<String>,
    jurisdictions: List<String>,
    utcDateToRun: LocalDate
): UtcTimeToRunReportQuery(repository, dataStreamIds, dataStreamRoutes, jurisdictions, utcDateToRun) {

    class Builder(
        repository: ProcessingStatusRepository
    ): UtcTimeToRunReportQuery.Builder<Builder>(repository) {
        override fun build() = UploadMetricsQuery(
            repository, dataStreamIds, dataStreamRoutes, jurisdictions, utcDateToRun
        )
    }

    override fun buildSql(): String {
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
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.UPLOAD_COMPLETED}' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    - 
                    MIN(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.METADATA_VERIFY}' THEN ${cPrefix}stageInfo.start_processing_time END) 
                    AS upload_delta,
                    
                    -- Delivery time including latency delta (end of the copy minus the end of the upload completion times)
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.FILE_DELIVERY}' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    - 
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.UPLOAD_COMPLETED}' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    AS delivery_delta,
                    
                    -- Total time delta (end to end duration)
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.FILE_DELIVERY}' THEN ${cPrefix}stageInfo.end_processing_time END) 
                    - 
                    MIN(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.METADATA_VERIFY}' THEN ${cPrefix}stageInfo.start_processing_time END) 
                    AS total_delta,
                    
                    -- File size
                    MAX(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.UPLOAD_STATUS}' THEN ${cPrefix}content.size END) AS file_size
            
                FROM $collectionName $cVar
                WHERE ${cPrefix}stageInfo.action IN ${openBkt}'${StageAction.METADATA_VERIFY}', '${StageAction.UPLOAD_COMPLETED}', '${StageAction.UPLOAD_STATUS}', '${StageAction.FILE_DELIVERY}'${closeBkt}
            """)

        querySB.append(whereClause(utcDateToRun))

        querySB.append("""
                GROUP BY ${cPrefix}uploadId
            ) AS upload_metrics;
        """)

        return querySB.toString().trimIndent()
    }

    fun run() = runQuery(UploadMetrics::class.java).first()
}