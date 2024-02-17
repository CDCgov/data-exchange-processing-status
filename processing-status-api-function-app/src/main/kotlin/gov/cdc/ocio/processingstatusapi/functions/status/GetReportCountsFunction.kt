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
        val reportsSqlQuery = "select count(1) as counts, r.stageName, r.content.schema_name, r.content.schema_version from $reportsContainerName r where r.uploadId = '$uploadId' group by r.stageName, r.content.schema_name, r.content.schema_version"

        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            StageCounts::class.java
        )
        if (reportItems.count() > 0) {

            val firstReportSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId' offset 0 limit 1"

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
                reportItems.forEach { stageCounts ->
                    getCounts(listOf(uploadId), stageCounts)?.let { this.stages[stageCounts.stageName!!] = it }
                }
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
     * @param uploadId String
     * @param stageCounts StageCounts
     */
    private fun getCounts(uploadIdsList: List<String>, stageCounts: StageCounts): Any? {
        var stageCountsObj: Any? = null
        val stageName = stageCounts.stageName
        if (stageName != null && stageCounts.counts != null) {
            // Check if further counting needed based on schema name
            when (stageCounts.schema_name) {
                HL7Debatch.schemaDefinition.schemaName -> {
                    val schemaName = HL7Debatch.schemaDefinition.schemaName
                    val quotedUploadIds = uploadIdsList.joinToString("\",\"", "\"", "\"")
                    val hl7DebatchCountsQuery =
                        "select SUM(r.content.number_of_messages) as numberOfMessages, SUM(r.content.number_of_messages_not_propagated) as numberOfMessagesNotPropagated from $reportsContainerName r where r.stageName = '$stageName' and r.content.schema_name = '$schemaName' and r.uploadId in ($quotedUploadIds)"

                    val hl7DebatchCountsItems = reportsContainer.queryItems(
                        hl7DebatchCountsQuery, CosmosQueryRequestOptions(),
                        HL7DebatchCounts::class.java
                    )

                    val firstItem = hl7DebatchCountsItems.firstOrNull()
                    stageCountsObj = if (firstItem != null) {
                        mapOf(
                            "counts" to stageCounts.counts,
                            "number_of_messages" to firstItem.numberOfMessages.toLong(),
                            "number_of_messages_not_propagated" to firstItem.numberOfMessagesNotPropagated.toLong()
                        )
                    } else
                        stageCounts.counts!!
                }
                // No further counts needed
                else -> {
                    stageCountsObj = stageCounts.counts!!
                }
            }
        }
        return stageCountsObj
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
        uploadIdsSqlQuery.append("select distinct value r.uploadId from $reportsContainerName r where r.destinationId = '$destinationId' and r.eventType = '$eventType'")

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
            logger.info("found upload ids = ${uploadIdsList.size}")

            val quotedUploadIds = uploadIdsList.joinToString("\",\"", "\"", "\"")
            val reportsSqlQuery = "select r.uploadId, count(r.stageName) as counts, r.stageName from Reports r where r.uploadId in ($quotedUploadIds) group by r.uploadId, r.stageName"

            logger.info("report query str = $reportsSqlQuery")

            val reportItems = reportsContainer.queryItems(
                reportsSqlQuery, CosmosQueryRequestOptions(),
                StageCountsForUpload::class.java
            )

            if (reportItems.count() > 0) {
                val uploads = mutableMapOf<String, MutableMap<String, Any>>()
                reportItems.forEach {
                    it.uploadId?.let { uploadId ->
                        var stageCountsMap = uploads[uploadId]
                        if (stageCountsMap == null)
                            stageCountsMap = mutableMapOf()
                        if (it.stageName != null && it.counts != null) {
                            val stageName = it.stageName!!
                            stageCountsMap[stageName] = it.counts!!
                            uploads[uploadId] = stageCountsMap
                        }
                    }
                }
                val reportCountsList = mutableListOf<ReportCounts>()
                uploads.forEach { upload ->
                    reportCountsList.add(ReportCounts().apply {
                        this.uploadId = upload.key
                        this.destinationId = destinationId
                        this.eventType = eventType
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