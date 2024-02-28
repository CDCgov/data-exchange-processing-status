package gov.cdc.ocio.processingstatusapi.functions.status

import com.azure.cosmos.models.CosmosQueryRequestOptions
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
import mu.KotlinLogging
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
                this.destinationId = firstReport?.destinationId
                this.eventType = firstReport?.eventType
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
            + "r.uploadId, r.stageName, SUM(r.content.number_of_messages) as numberOfMessages, "
            + "SUM(r.content.number_of_messages_not_propagated) as numberOfMessagesNotPropagated "
            + "from $reportsContainerName r "
            + "where r.content.schema_name = '$hl7DebatchSchemaName' and r.uploadId in ($quotedUploadIds) "
            + "group by r.uploadId, r.stageName"
        )
        val hl7DebatchCountsItems = reportsContainer.queryItems(
            hl7DebatchCountsQuery, CosmosQueryRequestOptions(),
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
            hl7ValidationCountsQuery, CosmosQueryRequestOptions(),
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

        val destinationId = request.queryParameters["destination_id"]
        val eventType = request.queryParameters["ext_event"]

        val dateStart = request.queryParameters["date_start"]
        val dateEnd = request.queryParameters["date_end"]

        if (destinationId == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("destination_id is required")
                .build()
        }

        if (eventType == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("ext_event is required")
                .build()
        }

        val uploadIdsSqlQuery = StringBuilder()
        uploadIdsSqlQuery.append(
            "select "
            + "distinct value r.uploadId "
            + "from $reportsContainerName r "
            + "where r.destinationId = '$destinationId' and r.eventType = '$eventType'"
        )

        dateStart?.run {
            try {
                val dateStartEpochSecs = DateUtils.getEpochFromDateString(dateStart, "date_start")
                uploadIdsSqlQuery.append(" and r._ts >= $dateStartEpochSecs")
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
                uploadIdsSqlQuery.append(" and r._ts < $dateEndEpochSecs")
            } catch (e: BadRequestException) {
                logger.error(e.localizedMessage)
                return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(e.localizedMessage)
                    .build()
            }
        }

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
                var earliestTimestamp: Date? = null
                reportItems.forEach {
                    val list = stageCountsByUploadId[it.uploadId!!] ?: mutableListOf()
                    list.add(StageCounts().apply {
                        this.schema_name = it.schema_name
                        this.schema_version = it.schema_version
                        this.counts = it.counts
                        this.stageName = it.stageName
                        it.timestamp?.let { timestamp ->
                            if (earliestTimestamp == null)
                                earliestTimestamp = timestamp
                            else if (timestamp.before(earliestTimestamp))
                                earliestTimestamp = timestamp
                        }
                    })
                    stageCountsByUploadId[it.uploadId!!] = list
                }
                val revisedStageCountsByUploadId = getCounts(stageCountsByUploadId)

                val reportCountsList = mutableListOf<ReportCounts>()
                revisedStageCountsByUploadId.forEach { upload ->
                    reportCountsList.add(ReportCounts().apply {
                        this.uploadId = upload.key
                        this.destinationId = destinationId
                        this.eventType = eventType
                        this.timestamp = earliestTimestamp
                        this.stages = upload.value
                    })
                }

                val aggregateReportCounts = AggregateReportCounts().apply {
                    this.summary = AggregateSummary().apply {
                        this.numUploads = reportCountsList.count()
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

        return request
            .createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("No results found")
            .build()
    }
}