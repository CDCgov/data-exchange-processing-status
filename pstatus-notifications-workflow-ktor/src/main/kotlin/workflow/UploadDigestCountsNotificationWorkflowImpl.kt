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
 * The implementation class which determines the daily digest counts of the list of jurisdictions for the set data stream list

 */
class UploadDigestCountsNotificationWorkflowImpl : UploadDigestCountsNotificationWorkflow, KoinComponent {
    private val repository by inject<ProcessingStatusRepository>()
    private val logger = KotlinLogging.logger {}
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
     *
     */
    override fun processDailyUploadDigest(
        jurisdictionIds: List<String>,
        dataStreams: List<String>,
        deliveryReference: String
    ) {
        try {
            val uploadDigestResults = getUploadDigest(jurisdictionIds,dataStreams)
            // Aggregate the upload counts
            val aggregatedCounts = aggregateUploadCounts(uploadDigestResults)
            // Format the email body
            val emailBody = formatEmailBody(aggregatedCounts)
            activities.sendDigestEmail(emailBody, deliveryReference)
        }
        catch (e: Exception) {
            logger.error("Error occurred while processing daily upload digest: ${e.message}")
            throw e
        }
    }


    /**
     * The function which determines the digest counts and top errors during an upload and its frequency
     * @param jurisdictionIds List<String>
     * @param dataStreams List<String>
     */
    private fun getUploadDigest(
        jurisdictionIds: List<String>,
        dataStreams: List<String>,
        ): List<UploadDigestResponse> {
        try {
            val query = buildDigestQuery(jurisdictionIds, dataStreams)
            return repository.reportsCollection.queryItems(query, UploadDigestResponse::class.java)

        } catch (e: Exception) {
            logger.error("Error occurred while checking for counts and top errors and frequency in an upload: ${e.message}")
            throw e
        }
    }

    /**
     * Function which uses SQL-compatible query statement in PartiQL that groups by jurisdictionId and dataStream to aggregate counts.
     * @param jurisdictionIds List<String>
     * @param dataStreamIds List<String>
     * @return String
     */

    private fun buildDigestQuery(jurisdictionIds: List<String>, dataStreamIds: List<String>): String {
        val jurisdictionIdsList = jurisdictionIds.joinToString(", ") { "'$it'" }
        val dataStreamsList = dataStreamIds.joinToString(", ") { "'$it'" }

        val reportsCollection =repository.reportsCollection
        val collectionName =reportsCollection.collectionNameForQuery
        val cVar = reportsCollection.collectionVariable
        val cPrefix =reportsCollection.collectionVariablePrefix

        return """
        SELECT  ${cPrefix}id,  ${cPrefix}jurisdiction, ${cPrefix}dataStreamId
        FROM $collectionName $cVar
        WHERE  ${cPrefix}jurisdiction IN ($jurisdictionIdsList) AND  ${cPrefix}dataStreamId IN ($dataStreamsList)
       
    """.trimIndent()

    }

    private fun aggregateUploadCounts(uploadCounts: List<UploadDigestResponse>): Map<String, Map<String, Int>> {
        return uploadCounts.groupBy { it.jurisdiction }
            .mapValues { (_, counts) ->
                counts.groupBy { it.dataStreamId }
                    .mapValues { (_, streamCounts) -> streamCounts.count()}
            }
    }

    private fun formatEmailBody(uploadCounts: Map<String, Map<String, Int>>): String {
        val builder = StringBuilder()
        builder.append("Daily Upload Digest for Data Streams:\n\n")

        uploadCounts.forEach { (jurisdictionId, streams) ->
            builder.append("Jurisdiction: $jurisdictionId\n")
            streams.forEach { (stream, count) ->
                builder.append("DataStreamId: $stream: $count uploads\n")
            }
            builder.append("\n")
        }
        return builder.toString()
    }
}
