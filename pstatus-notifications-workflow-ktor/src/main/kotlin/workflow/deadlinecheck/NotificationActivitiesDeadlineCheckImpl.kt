package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.activity.DataRequest
import gov.cdc.ocio.processingnotifications.activity.DataResponse
import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.activity.ResultWrapper
import gov.cdc.ocio.processingnotifications.query.DeadlineCheckQuery
import gov.cdc.ocio.processingnotifications.query.DeadlineCompliance
import gov.cdc.ocio.processingnotifications.query.UploadDigestCountsQuery
import io.temporal.failure.ActivityFailure
import mu.KotlinLogging
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.LocalTime


class NotificationActivitiesDeadlineCheckImpl : NotificationActivitiesImpl() {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    /**
     * Collects data for a specific deadline compliance check request.
     *
     * This method processes a `DeadlineCheckRequest` to evaluate upload compliance for a given data stream
     * and its associated jurisdictions. It computes the results of the deadline check and generates metadata
     * about uploads for each jurisdiction.
     *
     * @param request The data request object containing details for the deadline compliance check.
     *                Must be of type `DeadlineCheckRequest`.
     * @return A result wrapper containing the deadline compliance check response on success, or an encapsulated error on failure.
     */
    override fun collectData(request: DataRequest): ResultWrapper<DataResponse> {
        val deadlineCheckRequest = request as DeadlineCheckRequest

        val (deadlineCheckResults, jurisdictionCounts) = performaDeadlineCheck(
            deadlineCheckRequest.dataStreamId,
            deadlineCheckRequest.dataStreamRoute,
            deadlineCheckRequest.expectedJurisdictions,
            deadlineCheckRequest.deadlineTime
        )

        return ResultWrapper.Success(DeadlineCheckResponse(deadlineCheckResults, jurisdictionCounts))
    }

    /**
     * Checks whether an upload has occurred within a specified time range for the given data stream and jurisdiction.
     *
     * The method generates a query to search for uploads in the database, using today's date and a specific
     * start and end time range in UTC. If any matching results are found, the method returns true; otherwise, false.
     * In case of any errors during execution, an exception is thrown.
     *
     * @param dataStreamId The identifier of the data stream to check for uploads.
     * @param jurisdictions The jurisdictions to check for.
     * @return True if an upload is found within the specified time range, otherwise false.
     */
    private fun performaDeadlineCheck(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdictions: List<String>,
        deadlineTime: LocalTime
    ): Pair<DeadlineCompliance, Map<String, JurisdictionFacts>> {

        val deadlineCheckQuery = DeadlineCheckQuery.Builder(repository)
            .withDataStreamId(dataStreamId)
            .withDataStreamRoute(dataStreamRoute)
            .withExpectedJurisdictions(jurisdictions)
            .withDeadlineTime(deadlineTime)
            .build()

        val uploadDigestQuery = UploadDigestCountsQuery.Builder(repository)
            .withDataStreamIds(listOf(dataStreamId))
            .withDataStreamRoutes(listOf(dataStreamRoute))
            .withUtcToRun(LocalDate.now())
            .build()

        try {
            val deadlineCheckResults = deadlineCheckQuery.run()
            logger.info("Missing jurisdictions: $deadlineCheckResults.missingJurisdictions, late jurisdictions: $deadlineCheckResults.lateJurisdictions")

            val uploadDigestResults = uploadDigestQuery.run()
            val jurisdictionCounts = mapOf<String, JurisdictionFacts>().toMutableMap()
            uploadDigestResults.forEach {
                jurisdictionCounts[it.jurisdiction] = JurisdictionFacts(it.completed, it.lastUploadCompletedTime)
            }
            return Pair(deadlineCheckResults, jurisdictionCounts)
        } catch (ex: ActivityFailure) {
            logger.error("Error while processing deadline check. The workflow may have been canceled. Error: ${ex.localizedMessage}")
            throw ex
        } catch (ex: Exception) {
            logger.error("Error while processing deadline check: ${ex.localizedMessage}")
            throw ex
        }
    }
}