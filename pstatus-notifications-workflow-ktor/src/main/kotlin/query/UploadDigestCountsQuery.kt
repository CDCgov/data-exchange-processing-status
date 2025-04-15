package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestResponse
import java.time.LocalDate


class UploadDigestCountsQuery(
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
            SELECT
                ${cPrefix}dataStreamId, ${cPrefix}dataStreamRoute, ${cPrefix}jurisdiction,
            
                SUM(CASE WHEN ${cPrefix}stageInfo.action = 'upload-started' AND ${cPrefix}stageInfo.status = 'SUCCESS' THEN 1 ELSE 0 END) AS started,
                SUM(CASE WHEN ${cPrefix}stageInfo.action = 'upload-completed' AND ${cPrefix}stageInfo.status = 'SUCCESS' THEN 1 ELSE 0 END) AS completed,
                SUM(CASE WHEN ${cPrefix}stageInfo.action = 'blob-file-copy' AND ${cPrefix}stageInfo.status != 'SUCCESS' THEN 1 ELSE 0 END) AS failedDelivery,
                SUM(CASE WHEN ${cPrefix}stageInfo.action = 'blob-file-copy' AND ${cPrefix}stageInfo.status = 'SUCCESS' THEN 1 ELSE 0 END) AS delivered
            
            FROM $collectionName $cVar
            WHERE ${cPrefix}stageInfo.action IN ${openBkt}'upload-started', 'upload-completed', 'blob-file-copy'${closeBkt}
            """)

        querySB.append(whereClause(utcDateToRun, dataStreamIds, dataStreamRoutes, jurisdictions))

        querySB.append("""
            GROUP BY ${cPrefix}dataStreamId, ${cPrefix}dataStreamRoute, ${cPrefix}jurisdiction
            """)

        return querySB.toString().trimIndent()
    }

    /**
     * The function which gets the digest counts query and sends it to the corresponding db collection.
     *
     * @param utcDateToRun LocalDate
     * @param dataStreamIds List<String>
     * @param dataStreamRoutes List<String>
     * @param jurisdictions List<String>
     */
    fun run(
        utcDateToRun: LocalDate,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
    ): List<UploadDigestResponse> {
        return runCatching {
            val query = build(
                utcDateToRun,
                dataStreamIds,
                dataStreamRoutes,
                jurisdictions
            )
            logger.info("Executing upload digest counts query")
            return@runCatching collection.queryItems(query, UploadDigestResponse::class.java)
        }.getOrElse {
            logger.error("Error occurred while getting a digest of upload counts: ${it.localizedMessage}")
            throw it
        }
    }
}