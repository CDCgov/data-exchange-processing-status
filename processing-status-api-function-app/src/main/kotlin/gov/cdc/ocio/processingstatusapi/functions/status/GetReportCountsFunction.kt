package gov.cdc.ocio.processingstatusapi.functions.status

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.model.*
import gov.cdc.ocio.processingstatusapi.model.reports.*
import gov.cdc.ocio.processingstatusapi.model.reports.stagereports.HL7Debatch
import gov.cdc.ocio.processingstatusapi.model.reports.stagereports.HL7Validation
import gov.cdc.ocio.processingstatusapi.model.traces.*
import gov.cdc.ocio.processingstatusapi.utils.DateUtils
import gov.cdc.ocio.processingstatusapi.utils.JsonUtils
import gov.cdc.ocio.processingstatusapi.utils.PageUtils
import mu.KotlinLogging
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*


/**
 * Collection of ways to get reports.
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property context ExecutionContext
 * @constructor
 */
class GetReportCountsFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {

    private val logger = KotlinLogging.logger {}

    private val reportsContainerName = "Reports"
    private val partitionKey = "/uploadId"

    private val reportsContainer by lazy {
        CosmosContainerManager.initDatabaseContainer(reportsContainerName, partitionKey)!!
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            Date::class.java,
            JsonUtils.GsonUTCDateAdapter()
        )
        .create()

    /**
     * Get report for the given upload ID.
     *
     * @param uploadId String
     * @return HttpResponseMessage
     */
    fun withUploadId(uploadId: String): HttpResponseMessage {

        // Get the reports
        val reportsSqlQuery = (
            "select "
            + "count(1) as counts, r.stageName, r.content.schema_name, r.content.schema_version "
            + "from $reportsContainerName r where r.uploadId = '$uploadId' "
            + "group by r.stageName, r.content.schema_name, r.content.schema_version"
        )
        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            StageCounts::class.java
        )
        if (reportItems.count() > 0) {

            // Order by timestamp (ascending) and grab the first one found, which will give us the earliest timestamp.
            val firstReportSqlQuery = (
                "select * from $reportsContainerName r where r.uploadId = '$uploadId' "
                + "order by r.timestamp asc offset 0 limit 1"
            )

            val firstReportItems = reportsContainer.queryItems(
                firstReportSqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
            )
            val firstReport = firstReportItems.firstOrNull()

            logger.info("Successfully located report with uploadId = $uploadId")

            val reportResult = ReportCounts().apply {
                this.uploadId = uploadId
                this.dataStreamId = firstReport?.dataStreamId
                this.dataStreamRoute = firstReport?.dataStreamRoute
                this.timestamp = firstReport?.timestamp
                val stageCountsByUploadId = mapOf(uploadId to reportItems.toList())
                val revisedStageCountsByUploadId = getCounts(stageCountsByUploadId)
                val revisedStageCounts = revisedStageCountsByUploadId[uploadId]
                revisedStageCounts?.let { this.stages = it }
            }

            return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(gson.toJson(reportResult))
                .build()
        }

        logger.error("Failed to locate report with uploadId = $uploadId")

        return request
            .createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("Invalid uploadId provided")
            .build()
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
            + "from $reportsContainerName r "
            + "where r.content.schema_name = '$hl7DebatchSchemaName' and r.uploadId in ($quotedUploadIds) "
            + "group by r.uploadId, r.stageName"
        )

        val options = CosmosQueryRequestOptions()
        if (stageCountsByUploadId.size == 1)
            options.partitionKey = PartitionKey(stageCountsByUploadId.keys.first())
        else
            options.maxDegreeOfParallelism = -1 // let SDK decide optimal number of concurrent operations

        val hl7DebatchCountsItems = reportsContainer.queryItems(
            hl7DebatchCountsQuery, options,
            HL7DebatchCounts::class.java
        )

        val hl7ValidationSchemaName = HL7Validation.schemaDefinition.schemaName
        val hl7ValidationCountsQuery = (
            "select "
            + "r.uploadId, r.stageName, "
            + "count(contains(upper(r.content.summary.current_status), 'VALID') ? 1 : undefined) as valid, "
            + "count(not contains(upper(r.content.summary.current_status), 'VALID') ? 1 : undefined) as invalid "
            + "from $reportsContainerName r "
            + "where r.content.schema_name = '$hl7ValidationSchemaName' and r.uploadId in ($quotedUploadIds) "
            + "group by r.uploadId, r.stageName"
        )

        val hl7ValidationCountsItems = reportsContainer.queryItems(
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
     * @return HttpResponseMessage
     */
    fun withQueryParams(): HttpResponseMessage {

        val dataStreamId = request.queryParameters["data_stream_id"]
        val dataStreamRoute = request.queryParameters["data_stream_route"]

        val pageSize = request.queryParameters["page_size"]
        val pageNumber = request.queryParameters["page_number"]

        val dateStart = request.queryParameters["date_start"]
        val dateEnd = request.queryParameters["date_end"]

        val daysInterval = request.queryParameters["days_interval"]

        if (dataStreamId == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("data_stream_id is required")
                .build()
        }

        if (dataStreamRoute == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("data_stream_route is required")
                .build()
        }

        val pageUtils = PageUtils.Builder()
            .setMaxPageSize(500)
            .setDefaultPageSize(100)
            .build()

        val pageSizeAsInt = try {
            pageUtils.getPageSize(pageSize)
        } catch (ex: BadRequestException) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(ex.localizedMessage)
                .build()
        }

        if (!daysInterval.isNullOrBlank() && (!dateStart.isNullOrBlank() || !dateEnd.isNullOrBlank())) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("date_interval and date_start/date_end can't be used simultaneously")
                .build()
        }

        val timeRangeSqlPortion = StringBuilder()
        if (!daysInterval.isNullOrBlank()) {
            val dateStartEpochSecs = DateTime
                .now(DateTimeZone.UTC)
                .minusDays(Integer.parseInt(daysInterval))
                .withTimeAtStartOfDay()
                .toDate()
                .time / 1000
            timeRangeSqlPortion.append(" and r._ts >= $dateStartEpochSecs")
        } else {
            dateStart?.run {
                try {
                    val dateStartEpochSecs = DateUtils.getEpochFromDateString(dateStart, "date_start")
                    timeRangeSqlPortion.append(" and r._ts >= $dateStartEpochSecs")
                } catch (e: BadRequestException) {
                    logger.error(e.localizedMessage)
                    return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(e.localizedMessage)
                        .build()
                }
            }
            dateEnd?.run {
                try {
                    val dateEndEpochSecs = DateUtils.getEpochFromDateString(dateEnd, "date_end")
                    timeRangeSqlPortion.append(" and r._ts < $dateEndEpochSecs")
                } catch (e: BadRequestException) {
                    logger.error(e.localizedMessage)
                    return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(e.localizedMessage)
                        .build()
                }
            }
        }

        // Get the total matching upload ids
        val uploadIdCountSqlQuery = StringBuilder()
        uploadIdCountSqlQuery.append(
            "select "
                + "value count(1) "
                + "from (select distinct r.uploadId from $reportsContainerName r "
                + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' $timeRangeSqlPortion)"
        )

        val uploadIdCountResult = reportsContainer.queryItems(
            uploadIdCountSqlQuery.toString(), CosmosQueryRequestOptions(),
            Long::class.java
        )
        val totalItems = uploadIdCountResult.first()
        val numberOfPages: Int
        val pageNumberAsInt: Int
        val reportCountsList = mutableListOf<ReportCounts>()
        if (totalItems > 0L) {
            numberOfPages = (totalItems / pageSizeAsInt + if (totalItems % pageSizeAsInt > 0) 1 else 0).toInt()

            pageNumberAsInt = try {
                PageUtils.getPageNumber(pageNumber, numberOfPages)
            } catch (ex: BadRequestException) {
                return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(ex.localizedMessage)
                    .build()
            }
            val offset = (pageNumberAsInt - 1) * pageSizeAsInt

            val uploadIdsSqlQuery = StringBuilder()
            uploadIdsSqlQuery.append(
                "select "
                    + "distinct value r.uploadId "
                    + "from $reportsContainerName r "
                    + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' "
                    + "$timeRangeSqlPortion offset $offset limit $pageSizeAsInt"
            )

            // Get the matching uploadIds
            val uploadIds = reportsContainer.queryItems(
                uploadIdsSqlQuery.toString(), CosmosQueryRequestOptions(),
                String::class.java
            )

            if (uploadIds.count() > 0) {
                val uploadIdsList = uploadIds.toList()
                val quotedUploadIds = uploadIdsList.joinToString("\",\"", "\"", "\"")
                val reportsSqlQuery = (
                    "select "
                        + "r.uploadId, r.content.schema_name, r.content.schema_version, MIN(r.timestamp) as timestamp, count(r.stageName) as counts, r.stageName "
                        + "from $reportsContainerName r where r.uploadId in ($quotedUploadIds) "
                        + "group by r.uploadId, r.stageName, r.content.schema_name, r.content.schema_version"
                )
                val reportItems = reportsContainer.queryItems(
                    reportsSqlQuery, CosmosQueryRequestOptions(),
                    StageCountsForUpload::class.java
                )

                if (reportItems.count() > 0) {
                    val stageCountsByUploadId = mutableMapOf<String, MutableList<StageCounts>>()
                    val earliestTimestampByUploadId = mutableMapOf<String, Date>()
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
                                    else if (timestamp.before(earliestTimestamp))
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
                this.pageSize = pageSizeAsInt
                this.totalItems = totalItems
            }
            this.reportCountsList = reportCountsList
        }

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(gson.toJson(aggregateReportCounts))
            .build()
    }
}