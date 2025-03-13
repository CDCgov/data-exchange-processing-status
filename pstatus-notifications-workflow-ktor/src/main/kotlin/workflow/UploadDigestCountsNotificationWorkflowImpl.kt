package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.model.UploadDigestResponse
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration


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
     * @param dataStreamIds: List<String>
     * @param dataStreamRoutes: List<String>
     * @param jurisdictions: List<String>
     * @param emailAddresses List<String>
     */
    override fun processDailyUploadDigest(
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
        emailAddresses: List<String>
    ) {
        try {
            val uploadDigestResults = getUploadDigest(dataStreamIds, dataStreamRoutes, jurisdictions)

            if (uploadDigestResults.isNotEmpty()) {
                // Aggregate the upload counts
                val aggregatedCounts = aggregateUploadCounts(uploadDigestResults)
                // Format the email body
                val emailBody = formatEmailBody(aggregatedCounts)
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
     * @param dataStreamIds List<String>
     * @param dataStreamRoutes List<String>
     * @param jurisdictions List<String>
     */
    private fun getUploadDigest(
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
    ): List<UploadDigestResponse> {
        try {
            val query = buildDigestQuery(dataStreamIds, dataStreamRoutes, jurisdictions)
            return repository.reportsCollection.queryItems(query, UploadDigestResponse::class.java)
        } catch (e: Exception) {
            logger.error("Error occurred while checking for counts and top errors and frequency in an upload: ${e.message}")
            throw e
        }
    }

    /**
     * Function which uses SQL-compatible query statement in PartiQL for dynamo or sql statements for other db types.
     *
     * @param dataStreamIds List<String>
     * @param dataStreamRoutes List<String>
     * @param jurisdictions List<String>
     * @return String
     */
    private fun buildDigestQuery(
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
    ): String {
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
            SELECT ${cPrefix}id, ${cPrefix}dataStreamId, ${cPrefix}dataStreamRoute, ${cPrefix}jurisdiction
            FROM $collectionName $cVar
            WHERE ${cPrefix}dataStreamId IN ${openBkt}$dataStreamIdsList${closeBkt}
            AND ${cPrefix}dataStreamRoute IN ${openBkt}$dataStreamRoutesList${closeBkt}
            AND ${cPrefix}jurisdiction IN ${openBkt}$jurisdictionIdsList${closeBkt}
        """.trimIndent()
    }

    /**
     *  DynamoDB does not support GROUP BY, so this function is used to aggregate the counts
     *  Function that groups by jurisdictionId and dataStreamId to aggregate counts.
     *
     *  @param uploadCounts List<UploadDigestResponse>
     *  @return Map<String, Map<String, Int>>
     */
    private fun aggregateUploadCounts(
        uploadCounts: List<UploadDigestResponse>
    ): Map<String, Map<String, Int>> {
        return uploadCounts.groupBy { it.jurisdiction }
            .mapValues { (_, counts) ->
                counts.groupBy { it.dataStreamId }
                    .mapValues { (_, streamCounts) -> streamCounts.count() }
            }
    }

    /**
     *  Function for Email Body Formatting
     *
     *  @param uploadCounts Map<String, Map<String, Int>>
     *  @return String
     */
    private fun formatEmailBody(uploadCounts: Map<String, Map<String, Int>>): String {
        val builder = StringBuilder()
        builder.append("Daily Upload Digest for Data Streams:\n\n")
        builder.append("JurisdictionIds:\n")
        uploadCounts.forEach { (jurisdictionId, streams) ->
            builder.append("\t$jurisdictionId\n")
            builder.append("DataStreamIds:\n")
            streams.forEach { (stream, count) ->
                builder.append("\t$stream: $count uploads\n")
            }
            builder.append("\n")
        }
        return builder.toString()
    }
}
