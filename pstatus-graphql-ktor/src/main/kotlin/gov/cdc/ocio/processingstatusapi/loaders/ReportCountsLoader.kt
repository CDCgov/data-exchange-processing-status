package gov.cdc.ocio.processingstatusapi.loaders

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.models.ReportCounts
import gov.cdc.ocio.database.models.dao.ReportDao
import gov.cdc.ocio.database.utils.SqlClauseBuilder
import gov.cdc.ocio.processingstatusapi.models.query.PageSummary
import gov.cdc.ocio.processingstatusapi.models.reports.*
import gov.cdc.ocio.processingstatusapi.utils.PageUtils
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.OffsetDateTime
import java.time.ZoneOffset


class ReportCountsLoader: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val reportsCollection = repository.reportsCollection

    private val cName = reportsCollection.collectionNameForQuery
    private val cVar = reportsCollection.collectionVariable
    private val cPrefix = reportsCollection.collectionVariablePrefix
    private val cElFunc = repository.reportsCollection.collectionElementForQuery

    /**
     * Get report for the given upload ID.
     *
     * @param uploadId String
     * @return ReportCounts
     */
    fun withUploadId(uploadId: String): ReportCounts? {

        // Get the reports
        val reportsSqlQuery = (
                "select "
                        + "count(1) as counts, ${cPrefix}stageName, ${cPrefix}content.content_schema_name, ${cPrefix}content.content_schema_version "
                        + "from $cName $cVar where ${cPrefix}uploadId = '$uploadId' "
                        + "group by ${cPrefix}stageName, ${cPrefix}content.${cElFunc("content_schema_name")}, ${cPrefix}content.${cElFunc("content_schema_version")}"
                )
        val reportItems = reportsCollection.queryItems(
            reportsSqlQuery,
            StageCounts::class.java
        )
        if (reportItems.isNotEmpty()) {

            // Order by timestamp (ascending) and grab the first one found, which will give us the earliest timestamp.
            val firstReportSqlQuery = (
                    "select * from $cName $cVar where ${cPrefix}uploadId = '$uploadId' "
                            + "order by ${cPrefix}timestamp asc offset 0 limit 1"
                    )

            val firstReportItems = reportsCollection.queryItems(
                firstReportSqlQuery,
                ReportDao::class.java
            )
            val firstReport = firstReportItems.firstOrNull()

            logger.info("Successfully located report with uploadId = $uploadId")

            val stageCountsByUploadId = mapOf(uploadId to reportItems.toList())
            val revisedStageCounts: Map<String, Any> = stageCountsByUploadId[uploadId]?.groupBy { it.stageName }?.mapKeys {
                it.key ?: "Unknown"
            }?.mapValues { stageEntry ->
                stageEntry.value.map { stageItem ->
                    mapOf(
                        "schema_name" to (stageItem.content_schema_name ?: "Unknown"),
                        "schema_version" to (stageItem.content_schema_version ?: "Unknown"),
                        "count" to stageItem.counts
                    )
                }
            } ?: mapOf()

            val reportResult = ReportCounts().apply {
                this.uploadId = uploadId
                this.dataStreamId = firstReport?.dataStreamId
                this.dataStreamRoute = firstReport?.dataStreamRoute
                this.timestamp = firstReport?.timestamp?.atOffset(ZoneOffset.UTC)
                revisedStageCounts.let { this.stages = it }
            }

            return reportResult
        }

        logger.error("Failed to locate report with uploadId = $uploadId")

        return null
    }


    /**
     * Get upload report counts for the given query parameters.
     *
     * @return AggregateReportCounts
     */
    fun withParams(dataStreamId: String,
                   dataStreamRoute: String,
                   dateStart: String?,
                   dateEnd: String?,
                   daysInterval: Int?,
                   pageSize: Int,
                   pageNumber: Int
    ): AggregateReportCounts {

        val pageUtils = PageUtils.Builder()
            .setMaxPageSize(500)
            .setDefaultPageSize(100)
            .build()

        val pageSizeAsInt = pageUtils.getPageSize(pageSize)

        val timeRangeWhereClause = SqlClauseBuilder.buildSqlClauseForDateRange(
            daysInterval,
            dateStart,
            dateEnd,
            cPrefix
        )

        // Get the total matching upload ids
        val uploadIdCountSqlQuery = StringBuilder()
        uploadIdCountSqlQuery.append(
            "select "
                    + "value count(1) "
                    + "from (select distinct ${cPrefix}uploadId from $cName $cVar "
                    + "where ${cPrefix}dataStreamId = '$dataStreamId' and "
                    + "${cPrefix}dataStreamRoute = '$dataStreamRoute' "
        )
        if(timeRangeWhereClause.isNotEmpty()) {
            uploadIdCountSqlQuery.append("and $timeRangeWhereClause)as distinctUploads")
        }
        else{
            uploadIdCountSqlQuery.append(")as distinctUploads")
        }

        val uploadIdCountResult = reportsCollection.queryItems(
            uploadIdCountSqlQuery.toString(),
            Long::class.java
        )
        val totalItems = uploadIdCountResult.first()
        val numberOfPages: Int
        val pageNumberAsInt: Int
        val reportCountsList = mutableListOf<ReportCounts>()
        if (totalItems > 0L) {
            numberOfPages =  (totalItems / pageSize + if (totalItems % pageSize > 0) 1 else 0).toInt()

            pageNumberAsInt = PageUtils.getPageNumber(pageNumber, numberOfPages)
            val offset = (pageNumberAsInt - 1) * pageSize

            val uploadIdsSqlQuery = StringBuilder()
            uploadIdsSqlQuery.append(
                "select "
                        + "distinct value ${cPrefix}uploadId "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute'"
            )

            if(timeRangeWhereClause.isNotEmpty()) {
                uploadIdCountSqlQuery.append(" and $timeRangeWhereClause offset $offset limit $pageSizeAsInt")
            }

            // Get the matching uploadIds
            val uploadIds = reportsCollection.queryItems(
                uploadIdsSqlQuery.toString(),
                String::class.java
            )

            if (uploadIds.isNotEmpty()) {
                val uploadIdsList = uploadIds.toList()
                val quotedUploadIds = uploadIdsList.joinToString("\",\"", "\"", "\"")
                val reportsSqlQuery = (
                        "select "
                                + "${cPrefix}uploadId, ${cPrefix}content.content_schema_name, ${cPrefix}content.content_schema_version, "
                                + "MIN(MILLIS_TO_STR(STR_TO_MILLIS(r.timestamp))) AS timestamp, count(${cPrefix}stageName) as counts, ${cPrefix}stageName "
                                + "from $cName $cVar where ${cPrefix}uploadId in [$quotedUploadIds] "
                                + "group by ${cPrefix}uploadId, ${cPrefix}stageName, ${cPrefix}content.content_schema_name, "
                                + "${cPrefix}content.content_schema_version"
                        )
                val reportItems = reportsCollection.queryItems(
                    reportsSqlQuery,
                    StageCountsForUpload::class.java
                )

                if (reportItems.isNotEmpty()) {
                    val stageCountsByUploadId = mutableMapOf<String, MutableList<StageCounts>>()
                    val earliestTimestampByUploadId = mutableMapOf<String, OffsetDateTime>()
                    reportItems.forEach {
                        val list = stageCountsByUploadId[it.uploadId!!] ?: mutableListOf()
                        list.add(StageCounts().apply {
                            this.content_schema_name = it.content_schema_name
                            this.content_schema_version = it.content_schema_version
                            this.counts = it.counts
                            this.stageName = it.stageName
                            it.timestamp?.let { timestamp ->
                                val uploadId = it.uploadId
                                uploadId?.let {
                                    var earliestTimestamp = earliestTimestampByUploadId[uploadId]
                                    if (earliestTimestamp == null)
                                        earliestTimestamp = timestamp
                                    else if (timestamp.isBefore(earliestTimestamp))
                                        earliestTimestamp = timestamp
                                    earliestTimestampByUploadId[uploadId] = earliestTimestamp
                                }
                            }
                        })
                        stageCountsByUploadId[it.uploadId!!] = list
                    }

                    stageCountsByUploadId.forEach { upload ->
                        val uploadId = upload.key
                        reportCountsList.add(ReportCounts().apply {
                            this.uploadId = uploadId
                            this.dataStreamId = dataStreamId
                            this.dataStreamRoute = dataStreamRoute
                            this.timestamp = earliestTimestampByUploadId[uploadId]

                            val stageCountsByUploadIdList = mapOf(uploadId to reportItems.toList())
                            val revisedStageCounts: Map<String, Any> =stageCountsByUploadIdList[uploadId]?.groupBy { it.stageName }?.mapKeys {
                            it.key ?: "Unknown"
                        }?.mapValues { stageEntry ->
                            stageEntry.value.map { stageItem ->
                                mapOf(
                                    "schema_name" to (stageItem.content_schema_name ?: "Unknown"),
                                    "schema_version" to (stageItem.content_schema_version ?: "Unknown"),
                                    "count" to stageItem.counts
                                )
                            }
                        } ?: mapOf()
                            revisedStageCounts.let { this.stages = it }
                        })
                    }
                }
            }
        } else {
            numberOfPages = 0
            pageNumberAsInt = 0
        }

        val aggregateReportCounts = AggregateReportCounts().apply {
            this.summary = PageSummary().apply {
                this.pageNumber = pageNumberAsInt
                this.numberOfPages = numberOfPages
                this.pageSize = pageSize
                this.totalItems = totalItems.toInt()
            }
            this.uploads = reportCountsList
        }

        return aggregateReportCounts
    }


    /**
     * Get processing counts
     *
     * @return ProcessingCounts
     */
    fun getProcessingCounts(
        dataStreamId: String,
        dataStreamRoute: String,
        dateStart: String?,
        dateEnd: String?,
        daysInterval: Int?
    ): ProcessingCounts {

        val timeRangeWhereClause =
            SqlClauseBuilder.buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd, cPrefix)

        // Get number completed uploading
        val numCompletedUploadingSqlQuery = (
                "select "
                        + "value count(1) "
                        + "from $cName $cVar "
                        + "where ${cPrefix}content.content_schema_name = 'upload-completed' and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause "
                        + "and (${cPrefix}content['offset'] is null or ${cPrefix}content['offset']= ${cPrefix}content.size)"

                )

        val completedUploadingCountResult = reportsCollection.queryItems(
            numCompletedUploadingSqlQuery,
            Long::class.java
        )
        val totalCompletedUploading = completedUploadingCountResult.firstOrNull() ?: 0

        val numUploadingSqlQuery = (
                "select "
                        + "value count(1) "
                        + "from $cName $cVar "
                        + "where ${cPrefix}content.content_schema_name = 'upload-started' and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause "
                        + "and (${cPrefix}content['offset'] is null or ${cPrefix}content['offset']!= ${cPrefix}content.size)"
                )

        val uploadingCountResult = reportsCollection.queryItems(
            numUploadingSqlQuery,
            Long::class.java
        )
        val totalUploading = uploadingCountResult.firstOrNull() ?: 0

        val numFailedSqlQuery = (
                "select "
                        + "value count(1) "
                        + "from $cName $cVar "
                        + "where ${cPrefix}content.content_schema_name = 'dex-metadata-verify' and ${cPrefix}content.issues != null and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause"
                )

        val failedCountResult = reportsCollection.queryItems(
            numFailedSqlQuery,
            Long::class.java
        )
        val totalFailed = failedCountResult.firstOrNull() ?: 0

        val counts = ProcessingCounts().apply {
            totalCounts = totalCompletedUploading + totalUploading + totalFailed
            statusCounts.apply {
                uploaded.counts = totalCompletedUploading
                failed.counts = totalFailed
                failed.reasons = mapOf("metadata" to totalFailed)
                uploading.counts = totalUploading
            }
        }

        return counts
    }

    /**
     * Get a rollup of the counts for the data stream provided.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param dateStart String?
     * @param dateEnd String?
     * @param daysInterval Int?
     * @return List<StageCounts>
     */
    fun rollupCountsByStage(
        dataStreamId: String,
        dataStreamRoute: String,
        dateStart: String?,
        dateEnd: String?,
        daysInterval: Int?
    ): List<StageCounts> {

        val timeRangeWhereClause =
            SqlClauseBuilder.buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd, cPrefix)

        val rollupCountsQuery = (
                "select "
                        + "${cPrefix}content.content_schema_name, ${cPrefix}content.content_schema_version, "
                        + "count(*) as counts, ${cPrefix}stageName "
                        + "from $cName $cVar where ${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause "
                        + "group by ${cPrefix}stageName, ${cPrefix}content.content_schema_name, ${cPrefix}content.content_schema_version"
                )

        val rollupCountsResult = reportsCollection.queryItems(
            rollupCountsQuery,
            StageCounts::class.java
        )

        val rollupCounts = mutableListOf<StageCounts>()
        if (rollupCountsResult.isNotEmpty()) {
            rollupCounts.addAll(rollupCountsResult.toList())
        }

        return rollupCounts
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
