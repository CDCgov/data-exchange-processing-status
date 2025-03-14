package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.model.UploadDigestResponse
import gov.cdc.ocio.types.InstantRange
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * The implementation class which determines the daily digest counts of the list of jurisdictions for the set data
 * stream id list.
 *
 * @property repository ProcessingStatusRepository
 * @property logger logger
 * @property activities T
 */
class UploadDigestCountsNotificationWorkflowImpl :
    UploadDigestCountsNotificationWorkflow, KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val activities = Workflow.newActivityStub(
        NotificationActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10)) // Set the start-to-close timeout
            .setScheduleToCloseTimeout(Duration.ofMinutes(1)) // Set the schedule-to-close timeout
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setMaximumAttempts(3) // Set retry options if needed
                    .build()
            )
            .build()
    )

    /**
     * The main function which is used by temporal workflow engine for orchestrating the daily upload digest counts.
     *
     * @param numDaysAgoToRun Long
     * @param dataStreamIds List<String>
     * @param dataStreamRoutes List<String>
     * @param jurisdictions List<String>
     * @param emailAddresses List<String>
     */
    override fun processDailyUploadDigest(
        numDaysAgoToRun: Long,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
        emailAddresses: List<String>
    ) {
        try {
            val utcDateToRun = LocalDate.now().minusDays(numDaysAgoToRun)
            val formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy")
            val dateToRun = utcDateToRun.format(formatter)
            val uploadDigestResults = getUploadDigest(utcDateToRun, dataStreamIds, dataStreamRoutes, jurisdictions)

            if (uploadDigestResults.isNotEmpty()) {
                // Aggregate the upload counts
                val aggregatedCounts = aggregateUploadCounts(uploadDigestResults)
                // Format the email body
                val emailBody = formatEmailBody(dateToRun, aggregatedCounts)
                activities.sendDigestEmail(emailBody, emailAddresses)
            }
        } catch (e: Exception) {
            logger.error("Error occurred while processing daily upload digest: ${e.message}")
            throw e
        }
    }

    /**
     * The function which gets the digest counts query and sends it to the corresponding db collection.
     *
     * @param utcDateToRun LocalDate
     * @param dataStreamIds List<String>
     * @param dataStreamRoutes List<String>
     * @param jurisdictions List<String>
     */
    private fun getUploadDigest(
        utcDateToRun: LocalDate,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
    ): List<UploadDigestResponse> {
        try {
            val query = buildDigestQuery(utcDateToRun, dataStreamIds, dataStreamRoutes, jurisdictions)
            return repository.reportsCollection.queryItems(query, UploadDigestResponse::class.java)
        } catch (e: Exception) {
            logger.error("Error occurred while checking for counts and top errors and frequency in an upload: ${e.message}")
            throw e
        }
    }

    /**
     * Function which uses SQL-compatible query statement in PartiQL for dynamo or sql statements for other db types.
     *
     * @param utcDateToRun LocalDate
     * @param dataStreamIds List<String>
     * @param dataStreamRoutes List<String>
     * @param jurisdictions List<String>
     * @return String
     */
    private fun buildDigestQuery(
        utcDateToRun: LocalDate,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
    ): String {
        val instantRange = InstantRange.fromLocalDate(utcDateToRun)
        val startEpoch = instantRange.start.epochSecond * 1000
        val endEpoch = instantRange.endInclusive.epochSecond * 1000

        val jurisdictionIdsList = jurisdictions.joinToString(", ") { "'$it'" }
        val dataStreamIdsList = dataStreamIds.joinToString(", ") { "'$it'" }
        val dataStreamRoutesList = dataStreamRoutes.joinToString(", ") { "'$it'" }

        val reportsCollection = repository.reportsCollection
        val collectionName = reportsCollection.collectionNameForQuery
        val cVar = reportsCollection.collectionVariable
        val cPrefix = reportsCollection.collectionVariablePrefix
        val openBkt = reportsCollection.openBracketChar
        val closeBkt = reportsCollection.closeBracketChar

        return """
            SELECT ${cPrefix}dataStreamId, ${cPrefix}dataStreamRoute, ${cPrefix}jurisdiction
            FROM $collectionName $cVar
            WHERE ${cPrefix}dataStreamId IN ${openBkt}$dataStreamIdsList${closeBkt}
            AND ${cPrefix}stageInfo.action = 'upload-completed' AND ${cPrefix}stageInfo.status = 'SUCCESS'
            AND ${cPrefix}dataStreamRoute IN ${openBkt}$dataStreamRoutesList${closeBkt}
            AND ${cPrefix}jurisdiction IN ${openBkt}$jurisdictionIdsList${closeBkt}
            AND ${cPrefix}dexIngestDateTime >= $startEpoch
            AND ${cPrefix}dexIngestDateTime < $endEpoch
        """.trimIndent()
    }

    /**
     *  DynamoDB does not support GROUP BY, so this function is used to aggregate the counts
     *  Function that groups by jurisdictionId and dataStreamId to aggregate counts.
     *
     *  @param uploadCounts List<UploadDigestResponse>
     *  @return Map<String, Map<String, Map<String, Int>>>
     */
    private fun aggregateUploadCounts(
        uploadCounts: List<UploadDigestResponse>
    ): Map<String/*dataStreamId*/, Map<String/*dataStreamRoute*/, Map<String/*jurisdiction*/, Int>>> {
        return uploadCounts.groupBy { it.dataStreamId }
            .mapValues { (_, counts) ->
                counts.groupBy { it.dataStreamRoute }
                    .mapValues { (_, routeCounts) ->
                        routeCounts.groupBy { it.jurisdiction }
                            .mapValues { (_, jurisdictionCounts) -> jurisdictionCounts.count() }
                    }
            }
    }

    /**
     * Function for Email Body Formatting
     *
     * @param runDateUtc String
     * @param uploadCounts Map<String, Map<String, Map<String, Int>>>
     * @return String HTML formatted email
     */
    private fun formatEmailBody(
        runDateUtc: String,
        uploadCounts: Map<String, Map<String, Map<String, Int>>>
    ): String {
        return buildString {
            appendHTML().html {
                body {
                    h2 { +"Daily Upload Digest for Data Streams" }
                    div { +"Date: $runDateUtc (12:00:00am through 12:59:59pm UTC)" }
                    br
                    h3 { +"Summary" }
                    table {
                        thead {
                            tr {
                                th { +"Data Stream ID" }
                                th { +"Data Stream Route" }
                                th { +"Jurisdictions" }
                                th { +"Upload Counts" }
                            }
                        }
                        tbody {
                            uploadCounts.forEach { (dataStreamId, dataStreamRoutes) ->
                                dataStreamRoutes.forEach { (dataStreamRoute, jurisdictions) ->
                                    tr {
                                        td { +dataStreamId }
                                        td { +dataStreamRoute }
                                        td { +jurisdictions.size.toString() }
                                        td { +jurisdictions.values.sum().toString() }
                                    }
                                }
                            }
                        }
                    }
                    br
                    h3 { +"Details" }
                    table {
                        thead {
                            tr {
                                th { +"Data Stream ID" }
                                th { +"Data Stream Route" }
                                th { +"Jurisdiction" }
                                th { +"Upload Counts" }
                            }
                        }
                        tbody {
                            uploadCounts.forEach { (dataStreamId, dataStreamRoutes) ->
                                dataStreamRoutes.forEach { (dataStreamRoute, jurisdictions) ->
                                    jurisdictions.forEach { (jurisdiction, count) ->
                                        tr {
                                            td { +dataStreamId }
                                            td { +dataStreamRoute }
                                            td { +jurisdiction }
                                            td { +count.toString() }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    p {
                        +("Subscriptions to this email are managed by the Public Health Data Observability (PHDO) "
                                + "Processing Status (PS) API.  Use the PS API GraphQL interface to unsubscribe."
                                )
                    }
                    p {
                        a(href = "https://cdcgov.github.io/data-exchange/") { +"Click here" }
                        +" for more information."
                    }
                }
            }
        }
    }
}
