package gov.cdc.ocio.processingstatusapi.loaders

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.models.ReportCounts
import gov.cdc.ocio.database.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.query.PageSummary
import gov.cdc.ocio.processingstatusapi.models.reports.*
import gov.cdc.ocio.processingstatusapi.models.reports.stagereports.HL7Debatch
import gov.cdc.ocio.processingstatusapi.models.reports.stagereports.HL7Redactor
import gov.cdc.ocio.processingstatusapi.models.reports.stagereports.HL7Validation
import gov.cdc.ocio.processingstatusapi.utils.PageUtils
import gov.cdc.ocio.processingstatusapi.utils.SqlClauseBuilder
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
                        + "count(1) as counts, ${cPrefix}stageName, ${cPrefix}content.schema_name, ${cPrefix}content.schema_version "
                        + "from $cName $cVar where ${cPrefix}uploadId = '$uploadId' "
                        + "group by ${cPrefix}stageName, ${cPrefix}content.${cElFunc("schema_name")}, ${cPrefix}content.${cElFunc("schema_version")}"
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

            val reportResult = ReportCounts().apply {
                this.uploadId = uploadId
                this.dataStreamId = firstReport?.dataStreamId
                this.dataStreamRoute = firstReport?.dataStreamRoute
                this.timestamp = firstReport?.timestamp?.atOffset(ZoneOffset.UTC)
                val stageCountsByUploadId = mapOf(uploadId to reportItems.toList())
                val revisedStageCountsByUploadId = getCounts(stageCountsByUploadId)
                val revisedStageCounts = revisedStageCountsByUploadId[uploadId]
                revisedStageCounts?.let { this.stages = it }
            }

            return reportResult
        }

        logger.error("Failed to locate report with uploadId = $uploadId")

        return null
    }

    /**
     * Get the counts, including any special counting based on the schema from the report, from the uploadIds and
     * stageCounts provided.
     *
     * @param stageCountsByUploadId Map<String, List<StageCounts>>
     * @return Map<String, Map<String, Any>> Revised stage counts by uploadId.  In the returned Map, the outer key is
     * the uploadId and outer value is the stage counts map.  The inner key is the stage name and the inner value is
     * the stage counts, which can be a simple integer or an object containing additional counts.
     */
    private fun getCounts(stageCountsByUploadId: Map<String, List<StageCounts>>): Map<String, Map<String, Any>> {

        val revisedStageCountsByUploadId = mutableMapOf<String, MutableMap<String, Any>>()

        val uploadIdList = stageCountsByUploadId.keys
        val quotedUploadIds = uploadIdList.joinToString("\",\"", "\"", "\"")

        // Get counts for any HL7 debatch stages
        val hl7DebatchSchemaName = HL7Debatch.schemaDefinition.schemaName
        val hl7DebatchCountsQuery = (
                "select "
                        + "${cPrefix}uploadId, ${cPrefix}stageName, SUM(${cPrefix}content.stage.report.number_of_messages) as numberOfMessages, "
                        + "SUM(${cPrefix}content.stage.report.number_of_messages_not_propagated) as numberOfMessagesNotPropagated "
                        + "from $cName $cVar "
                        + "where ${cPrefix}content.schema_name = '$hl7DebatchSchemaName' and ${cPrefix}uploadId in ($quotedUploadIds) "
                        + "group by ${cPrefix}uploadId, ${cPrefix}stageName"
                )

        val hl7DebatchCountsItems = reportsCollection.queryItems(
            hl7DebatchCountsQuery,
            HL7DebatchCounts::class.java
        )

        val hl7ValidationSchemaName = HL7Validation.schemaDefinition.schemaName
        val hl7ValidationCountsQuery = (
                "select "
                        + "${cPrefix}uploadId, ${cPrefix}stageName, "
                        + "count(contains(upper(${cPrefix}content.summary.current_status), 'VALID_') ? 1 : undefined) as valid, "
                        + "count(not contains(upper(${cPrefix}content.summary.current_status), 'VALID_') ? 1 : undefined) as invalid "
                        + "from $cName $cVar "
                        + "where ${cPrefix}content.schema_name = '$hl7ValidationSchemaName' and ${cPrefix}uploadId in ($quotedUploadIds) "
                        + "group by ${cPrefix}uploadId, ${cPrefix}stageName"
                )

        val hl7ValidationCountsItems = reportsCollection.queryItems(
            hl7ValidationCountsQuery,
            HL7ValidationCounts::class.java
        )

        stageCountsByUploadId.forEach { entry ->
            val uploadId = entry.key
            val stageCounts = entry.value
            val revisedStageCounts = revisedStageCountsByUploadId[uploadId] ?: mutableMapOf()
            stageCounts.forEach { stageCount ->
                when (stageCount.schema_name) {
                    HL7Debatch.schemaDefinition.schemaName -> {
                        val hl7DebatchCounts = hl7DebatchCountsItems.toList().firstOrNull {
                            it.uploadId == uploadId && it.stageName == stageCount.stageName
                        }
                        val counts: Any = if (hl7DebatchCounts != null) {
                            mapOf(
                                "counts" to stageCount.counts,
                                "number_of_messages" to hl7DebatchCounts.numberOfMessages.toLong(),
                                "number_of_messages_not_propagated" to hl7DebatchCounts.numberOfMessagesNotPropagated.toLong()
                            )
                        } else
                            stageCount.counts!!
                        revisedStageCounts[stageCount.stageName!!] = counts
                    }
                    HL7Validation.schemaDefinition.schemaName -> {
                        val hl7ValidationCounts = hl7ValidationCountsItems.toList().firstOrNull {
                            it.uploadId == uploadId && it.stageName == stageCount.stageName
                        }
                        val counts: Any = if (hl7ValidationCounts != null) {
                            mapOf(
                                "counts" to stageCount.counts,
                                "valid" to hl7ValidationCounts.valid.toLong(),
                                "invalid" to hl7ValidationCounts.invalid.toLong()
                            )
                        } else
                            stageCount.counts!!
                        revisedStageCounts[stageCount.stageName!!] = counts
                    }
                    // No further counts needed
                    else -> {
                        revisedStageCounts[stageCount.stageName!!] = stageCount.counts!!
                    }
                }
            }
            revisedStageCountsByUploadId[uploadId] = revisedStageCounts
        }

        return revisedStageCountsByUploadId
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

        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(
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
                    + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause)"
        )

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
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "$timeRangeWhereClause offset $offset limit $pageSizeAsInt"
            )

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
                                + "${cPrefix}uploadId, ${cPrefix}content.schema_name, ${cPrefix}content.schema_version, "
                                + "MIN(r.timestamp) as timestamp, count(${cPrefix}stageName) as counts, ${cPrefix}stageName "
                                + "from $cName $cVar where ${cPrefix}uploadId in ($quotedUploadIds) "
                                + "group by ${cPrefix}uploadId, ${cPrefix}stageName, ${cPrefix}content.schema_name, "
                                + "${cPrefix}content.schema_version"
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
                            this.schema_name = it.schema_name
                            this.schema_version = it.schema_version
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
                    val revisedStageCountsByUploadId = getCounts(stageCountsByUploadId)

                    revisedStageCountsByUploadId.forEach { upload ->
                        val uploadId = upload.key
                        reportCountsList.add(ReportCounts().apply {
                            this.uploadId = uploadId
                            this.dataStreamId = dataStreamId
                            this.dataStreamRoute = dataStreamRoute
                            this.timestamp = earliestTimestampByUploadId[uploadId]
                            this.stages = upload.value
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
     * Gets the total number of HL7 reports found with an invalid structure validation for the filter criteria
     * provided.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param dateStart String?
     * @param dateEnd String?
     * @param daysInterval String?
     * @return HL7InvalidStructureValidationCounts
     */
    fun getHL7InvalidStructureValidationCounts(dataStreamId: String,
                                               dataStreamRoute: String,
                                               dateStart: String?,
                                               dateEnd: String?,
                                               daysInterval: Int?
    ): HL7InvalidStructureValidationCounts {

        val timeRangeWhereClause =
            SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd, cPrefix)

        val reportsSqlQuery = (
                "select "
                        + "value count(not contains(upper(${cPrefix}content.summary.current_status), 'VALID_') ? 1 : undefined) "
                        + "from $cName $cVar "
                        + "where ${cPrefix}content.schema_name = '${HL7Validation.schemaDefinition.schemaName}' and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause"
                )

        val startTime = System.currentTimeMillis()
        val countResult = reportsCollection.queryItems(
            reportsSqlQuery,
            Long::class.java
        )
        val totalItems = countResult.firstOrNull() ?: 0
        val endTime = System.currentTimeMillis()
        val counts = HL7InvalidStructureValidationCounts().apply {
            this.counts = totalItems
            this.queryTimeMillis = endTime - startTime
        }

        return counts
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
            SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd, cPrefix)

        // Get number completed uploading
        val numCompletedUploadingSqlQuery = (
                "select "
                        + "value count(1) "
                        + "from $cName $cVar "
                        + "where ${cPrefix}content.schema_name = 'upload' and ${cPrefix}content['offset'] = ${cPrefix}content.size and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause"
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
                        + "where ${cPrefix}content.schema_name = 'upload' and ${cPrefix}content['offset'] != ${cPrefix}content.size and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause"
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
                        + "where ${cPrefix}content.schema_name = 'dex-metadata-verify' and ${cPrefix}content.issues != null and "
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
     * Get direct and in-direct message counts
     *
     * @return HttpResponseMessage
     */
    fun getHL7DirectIndirectMessageCounts(
        dataStreamId: String,
        dataStreamRoute: String,
        dateStart: String?,
        dateEnd: String?,
        daysInterval: Int?
    ): HL7DirectIndirectMessageCounts {

        val timeRangeWhereClause =
            SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd, cPrefix)

        val directMessageQuery = (
                "select value SUM(directCounts) "
                        + "from (select value SUM(${cPrefix}content.stage.report.number_of_messages) from $cName $cVar "
                        + "where ${cPrefix}content.schema_name = '${HL7Debatch.schemaDefinition.schemaName}' and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause) as directCounts"
                )

        val indirectMessageQuery = (
                "select value count(redactedCount) from ( "
                        + "select * from $cName $cVar where ${cPrefix}content.schema_name = '${HL7Redactor.schemaDefinition.schemaName}' and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause) as redactedCount"
                )

        val startTime = System.currentTimeMillis()

        val directCountResult = reportsCollection.queryItems(
            directMessageQuery,
            Float::class.java
        )

        val indirectCountResult = reportsCollection.queryItems(
            indirectMessageQuery,
            Float::class.java
        )

        val directTotalItems: Long = directCountResult.firstOrNull()?.toLong() ?: 0
        val inDirectTotalItems = indirectCountResult.firstOrNull()?.toLong() ?: 0

        val endTime = System.currentTimeMillis()
        val counts = HL7DirectIndirectMessageCounts().apply {
            this.directCounts = directTotalItems
            this.indirectCounts = inDirectTotalItems
            this.queryTimeMillis = endTime - startTime
        }

        return counts
    }

    /**
     * Get the number of invalid HL7v2 messages count using two different methods.
     *
     * @return HttpResponseMessage
     */
    fun getHL7InvalidMessageCounts(
        dataStreamId: String,
        dataStreamRoute: String,
        dateStart: String?,
        dateEnd: String?,
        daysInterval: Int?
    ) : HL7InvalidMessageCounts {

        val timeRangeWhereClause =
            SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd, cPrefix)

        val directInvalidMessageQuery = (
                "select value SUM(directCounts) "
                        + "FROM (select value SUM(${cPrefix}content.stage.report.number_of_messages) from $cName $cVar "
                        + "where ${cPrefix}content.schema_name = '${HL7Redactor.schemaDefinition.schemaName}' and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause) as directCounts"
                )

        val directStructureInvalidMessageQuery = (
                "select "
                        + "value count(not contains(upper(${cPrefix}content.summary.current_status), 'VALID_') ? 1 : undefined) "
                        + "from $cName $cVar "
                        + "where ${cPrefix}content.schema_name = '${HL7Validation.schemaDefinition.schemaName}' and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause"
                )

        val indirectInvalidMessageQuery = (
                "select value count(invalidCounts) from ("
                        + "select * from $cName $cVar where ${cPrefix}content.schema_name != 'HL7-JSON-LAKE-TRANSFORMER' and "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause) as invalidCounts"
                )

        val startTime = System.currentTimeMillis()

        val directRedactedCountResult = reportsCollection.queryItems(
            directInvalidMessageQuery,
            Float::class.java
        )

        val directStructureCountResult = reportsCollection.queryItems(
            directStructureInvalidMessageQuery,
            Float::class.java
        )

        val indirectCountResult = reportsCollection.queryItems(
            indirectInvalidMessageQuery,
            Float::class.java
        )

        val directRedactedCount = directRedactedCountResult.firstOrNull()?.toLong() ?: 0
        val directStructureCount = directStructureCountResult.firstOrNull()?.toLong() ?: 0
        val directTotalItems = directRedactedCount + directStructureCount
        val indirectTotalItems = indirectCountResult.firstOrNull()?.toLong() ?: 0

        val endTime = System.currentTimeMillis()

        val counts = HL7InvalidMessageCounts().apply {
            this.invalidMessageDirectCounts = directTotalItems
            this.invalidMessageIndirectCounts = indirectTotalItems
            this.queryTimeMillis = endTime - startTime
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
            SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd, cPrefix)

        val rollupCountsQuery = (
                "select "
                        + "${cPrefix}content.schema_name, ${cPrefix}content.schema_version, "
                        + "count(${cPrefix}stageName) as counts, ${cPrefix}stageName "
                        + "from $cName $cVar where ${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause "
                        + "group by ${cPrefix}stageName, ${cPrefix}content.schema_name, ${cPrefix}content.schema_version"
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
