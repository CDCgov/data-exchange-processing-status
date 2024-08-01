package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import gov.cdc.ocio.processingstatusapi.models.ReportCounts
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.query.PageSummary
import gov.cdc.ocio.processingstatusapi.models.reports.*
import gov.cdc.ocio.processingstatusapi.models.reports.stagereports.HL7Debatch
import gov.cdc.ocio.processingstatusapi.models.reports.stagereports.HL7Redactor
import gov.cdc.ocio.processingstatusapi.models.reports.stagereports.HL7Validation
import gov.cdc.ocio.processingstatusapi.utils.PageUtils
import gov.cdc.ocio.processingstatusapi.utils.SqlClauseBuilder
import java.time.OffsetDateTime

class ReportCountsLoader: CosmosLoader() {

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
                        + "count(1) as counts, r.stageName, r.content.schema_name, r.content.schema_version "
                        + "from r where r.uploadId = '$uploadId' "
                        + "group by r.stageName, r.content.schema_name, r.content.schema_version"
                )
        val reportItems = reportsContainer?.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            StageCounts::class.java
        )
        if (reportItems != null && reportItems.count() > 0) {

            // Order by timestamp (ascending) and grab the first one found, which will give us the earliest timestamp.
            val firstReportSqlQuery = (
                    "select * from r where r.uploadId = '$uploadId' "
                            + "order by r.timestamp asc offset 0 limit 1"
                    )

            val firstReportItems = reportsContainer?.queryItems(
                firstReportSqlQuery, CosmosQueryRequestOptions(),
                ReportDao::class.java
            )
            val firstReport = firstReportItems?.firstOrNull()

            logger.info("Successfully located report with uploadId = $uploadId")

            val reportResult = ReportCounts().apply {
                this.uploadId = uploadId
                this.dataStreamId = firstReport?.dataStreamId
                this.dataStreamRoute = firstReport?.dataStreamRoute
               // this.timestamp = firstReport?.timestamp?.toInstant()?.atOffset(ZoneOffset.UTC)
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
                        + "r.uploadId, r.stageName, SUM(r.content.stage.report.number_of_messages) as numberOfMessages, "
                        + "SUM(r.content.stage.report.number_of_messages_not_propagated) as numberOfMessagesNotPropagated "
                        + "from r "
                        + "where r.content.schema_name = '$hl7DebatchSchemaName' and r.uploadId in ($quotedUploadIds) "
                        + "group by r.uploadId, r.stageName"
                )

        val options = CosmosQueryRequestOptions()
        if (stageCountsByUploadId.size == 1)
            options.partitionKey = PartitionKey(stageCountsByUploadId.keys.first())
        else
            options.maxDegreeOfParallelism = -1 // let SDK decide optimal number of concurrent operations

        val hl7DebatchCountsItems = reportsContainer?.queryItems(
            hl7DebatchCountsQuery, options,
            HL7DebatchCounts::class.java
        )

        val hl7ValidationSchemaName = HL7Validation.schemaDefinition.schemaName
        val hl7ValidationCountsQuery = (
                "select "
                        + "r.uploadId, r.stageName, "
                        + "count(contains(upper(r.content.summary.current_status), 'VALID_') ? 1 : undefined) as valid, "
                        + "count(not contains(upper(r.content.summary.current_status), 'VALID_') ? 1 : undefined) as invalid "
                        + "from r "
                        + "where r.content.schema_name = '$hl7ValidationSchemaName' and r.uploadId in ($quotedUploadIds) "
                        + "group by r.uploadId, r.stageName"
                )

        val hl7ValidationCountsItems = reportsContainer?.queryItems(
            hl7ValidationCountsQuery, options,
            HL7ValidationCounts::class.java
        )

        stageCountsByUploadId.forEach { entry ->
            val uploadId = entry.key
            val stageCounts = entry.value
            val revisedStageCounts = revisedStageCountsByUploadId[uploadId] ?: mutableMapOf()
            stageCounts.forEach { stageCount ->
                when (stageCount.schema_name) {
                    HL7Debatch.schemaDefinition.schemaName -> {
                        val hl7DebatchCounts = hl7DebatchCountsItems?.toList()?.firstOrNull {
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
                        val hl7ValidationCounts = hl7ValidationCountsItems?.toList()?.firstOrNull {
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
            dateEnd
        )

        // Get the total matching upload ids
        val uploadIdCountSqlQuery = StringBuilder()
        uploadIdCountSqlQuery.append(
            "select "
                    + "value count(1) "
                    + "from (select distinct r.uploadId from r "
                    + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause)"
        )

        val uploadIdCountResult = reportsContainer?.queryItems(
            uploadIdCountSqlQuery.toString(), CosmosQueryRequestOptions(),
            Long::class.java
        )
        val totalItems = uploadIdCountResult?.first() ?: 0
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
                        + "distinct value r.uploadId "
                        + "from r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "$timeRangeWhereClause offset $offset limit $pageSizeAsInt"
            )

            // Get the matching uploadIds
            val uploadIds = reportsContainer?.queryItems(
                uploadIdsSqlQuery.toString(), CosmosQueryRequestOptions(),
                String::class.java
            )

            if (uploadIds != null && uploadIds.count() > 0) {
                val uploadIdsList = uploadIds.toList()
                val quotedUploadIds = uploadIdsList.joinToString("\",\"", "\"", "\"")
                val reportsSqlQuery = (
                        "select "
                                + "r.uploadId, r.content.schema_name, r.content.schema_version, MIN(r.timestamp) as timestamp, count(r.stageName) as counts, r.stageName "
                                + "from r where r.uploadId in ($quotedUploadIds) "
                                + "group by r.uploadId, r.stageName, r.content.schema_name, r.content.schema_version"
                        )
                val reportItems = reportsContainer?.queryItems(
                    reportsSqlQuery, CosmosQueryRequestOptions(),
                    StageCountsForUpload::class.java
                )

                if (reportItems != null && reportItems.count() > 0) {
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

        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd)

        val reportsSqlQuery = (
                "select "
                        + "value count(not contains(upper(r.content.summary.current_status), 'VALID_') ? 1 : undefined) "
                        + "from r "
                        + "where r.content.schema_name = '${HL7Validation.schemaDefinition.schemaName}' and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause"
                )

        val startTime = System.currentTimeMillis()
        val countResult = reportsContainer?.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            Long::class.java
        )
        val totalItems = countResult?.firstOrNull() ?: 0
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
    fun getProcessingCounts(dataStreamId: String,
                            dataStreamRoute: String,
                            dateStart: String?,
                            dateEnd: String?,
                            daysInterval: Int?
    ): ProcessingCounts {

        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd)

        // Get number completed uploading
        val numCompletedUploadingSqlQuery = (
                "select "
                        + "value count(1) "
                        + "from r "
                        + "where r.content.schema_name = 'upload' and r.content['offset'] = r.content.size and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause"
                )

        val completedUploadingCountResult = reportsContainer?.queryItems(
            numCompletedUploadingSqlQuery, CosmosQueryRequestOptions(),
            Long::class.java
        )
        val totalCompletedUploading = completedUploadingCountResult?.firstOrNull() ?: 0

        val numUploadingSqlQuery = (
                "select "
                        + "value count(1) "
                        + "from r "
                        + "where r.content.schema_name = 'upload' and r.content['offset'] != r.content.size and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause"
                )

        val uploadingCountResult = reportsContainer?.queryItems(
            numUploadingSqlQuery, CosmosQueryRequestOptions(),
            Long::class.java
        )
        val totalUploading = uploadingCountResult?.firstOrNull() ?: 0

        val numFailedSqlQuery = (
                "select "
                        + "value count(1) "
                        + "from r "
                        + "where r.content.schema_name = 'dex-metadata-verify' and r.content.issues != null and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause"
                )

        val failedCountResult = reportsContainer?.queryItems(
            numFailedSqlQuery, CosmosQueryRequestOptions(),
            Long::class.java
        )
        val totalFailed = failedCountResult?.firstOrNull() ?: 0

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

        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd)

        val directMessageQuery = (
                "select value SUM(directCounts) "
                        + "from (select value SUM(r.content.stage.report.number_of_messages) from r "
                        + "where r.content.schema_name = '${HL7Debatch.schemaDefinition.schemaName}' and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause) as directCounts"
                )

        val indirectMessageQuery = (
                "select value count(redactedCount) from ( "
                        + "select * from r where r.content.schema_name = '${HL7Redactor.schemaDefinition.schemaName}' and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause) as redactedCount"
                )

        val startTime = System.currentTimeMillis()

        val directCountResult = reportsContainer?.queryItems(
            directMessageQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val indirectCountResult = reportsContainer?.queryItems(
            indirectMessageQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val directTotalItems: Long = directCountResult?.firstOrNull()?.toLong() ?: 0
        val inDirectTotalItems = indirectCountResult?.firstOrNull()?.toLong() ?: 0

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

        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd)

        val directInvalidMessageQuery = (
                "select value SUM(directCounts) "
                        + " FROM (select value SUM(r.content.stage.report.number_of_messages) from r "
                        + " where r.content.schema_name = '${HL7Redactor.schemaDefinition.schemaName}' and "
                        + " r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause) as directCounts"
                )

        val directStructureInvalidMessageQuery = (
                "select "
                        + "value count(not contains(upper(r.content.summary.current_status), 'VALID_') ? 1 : undefined) "
                        + "from r "
                        + "where r.content.schema_name = '${HL7Validation.schemaDefinition.schemaName}' and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause"
                )

        val indirectInvalidMessageQuery = (
                "select value count(invalidCounts) from ("
                        + "select * from r where r.content.schema_name != 'HL7-JSON-LAKE-TRANSFORMER' and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause) as invalidCounts"
                )

        val startTime = System.currentTimeMillis()

        val directRedactedCountResult = reportsContainer?.queryItems(
            directInvalidMessageQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val directStructureCountResult = reportsContainer?.queryItems(
            directStructureInvalidMessageQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val indirectCountResult = reportsContainer?.queryItems(
            indirectInvalidMessageQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val directRedactedCount = directRedactedCountResult?.firstOrNull()?.toLong() ?: 0
        val directStructureCount = directStructureCountResult?.firstOrNull()?.toLong() ?: 0
        val directTotalItems = directRedactedCount + directStructureCount
        val indirectTotalItems = indirectCountResult?.firstOrNull()?.toLong() ?: 0

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

        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, dateStart, dateEnd)

        val rollupCountsQuery = (
                "select "
                        + "r.content.schema_name, r.content.schema_version, count(r.stageName) as counts, r.stageName "
                        + "from r where r.dataStreamId = '$dataStreamId' and "
                        + "r.dataStreamRoute = '$dataStreamRoute' and $timeRangeWhereClause "
                        + "group by r.stageName, r.content.schema_name, r.content.schema_version"
                )

        val rollupCountsResult = reportsContainer?.queryItems(
            rollupCountsQuery, CosmosQueryRequestOptions(),
            StageCounts::class.java
        )

        val rollupCounts = mutableListOf<StageCounts>()
        if (rollupCountsResult != null && rollupCountsResult.count() > 0) {
            rollupCounts.addAll(rollupCountsResult.toList())
        }

        return rollupCounts
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
