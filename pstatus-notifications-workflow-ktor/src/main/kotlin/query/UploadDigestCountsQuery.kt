package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.database.models.StageStatus
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestResponse
import java.time.LocalDate


/**
 * Upload counts query.
 *
 * @constructor
 */
class UploadDigestCountsQuery private constructor(
    repository: ProcessingStatusRepository,
    dataStreamIds: List<String>,
    dataStreamRoutes: List<String>,
    jurisdictions: List<String>,
    utcDateToRun: LocalDate
): UtcTimeToRunReportQuery("upload digest counts", repository, dataStreamIds, dataStreamRoutes, jurisdictions, utcDateToRun) {

    class Builder(
        repository: ProcessingStatusRepository
    ): UtcTimeToRunReportQuery.Builder<Builder>(repository) {
        override fun build() = UploadDigestCountsQuery(
            repository, dataStreamIds, dataStreamRoutes, jurisdictions, utcDateToRun
        )
    }

    override fun buildSql(): String {
        val querySB = StringBuilder()

        querySB.append("""
            SELECT
                ${cPrefix}dataStreamId, ${cPrefix}dataStreamRoute, ${cPrefix}jurisdiction,
            
                SUM(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.UPLOAD_STARTED}' AND ${cPrefix}stageInfo.status = '${StageStatus.SUCCESS}' THEN 1 ELSE 0 END) AS started,
                SUM(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.UPLOAD_COMPLETED}' AND ${cPrefix}stageInfo.status = '${StageStatus.SUCCESS}' THEN 1 ELSE 0 END) AS completed,
                SUM(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.FILE_DELIVERY}' AND ${cPrefix}stageInfo.status != '${StageStatus.SUCCESS}' THEN 1 ELSE 0 END) AS failedDelivery,
                SUM(CASE WHEN ${cPrefix}stageInfo.action = '${StageAction.FILE_DELIVERY}' AND ${cPrefix}stageInfo.status = '${StageStatus.SUCCESS}' THEN 1 ELSE 0 END) AS delivered
            
            FROM $collectionName $cVar
            WHERE ${cPrefix}stageInfo.action IN ${openBkt}'${StageAction.UPLOAD_STARTED}', '${StageAction.UPLOAD_COMPLETED}', '${StageAction.FILE_DELIVERY}'${closeBkt}
            """)

        querySB.append(whereClause(utcDateToRun))

        querySB.append("""
            GROUP BY ${cPrefix}dataStreamId, ${cPrefix}dataStreamRoute, ${cPrefix}jurisdiction
            """)

        return querySB.toString().trimIndent()
    }

    /**
     * The function which gets the digest counts query and sends it to the corresponding db collection.
     *
     * @return List<UploadDigestResponse>
     */
    fun run() = runQuery(UploadDigestResponse::class.java)
}