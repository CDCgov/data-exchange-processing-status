package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.activity.DataRequest
import gov.cdc.ocio.processingnotifications.activity.DataResponse
import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.activity.ResultWrapper
import gov.cdc.ocio.processingnotifications.query.UploadDigestCountsQuery
import gov.cdc.ocio.processingnotifications.query.UploadDurationQuery
import gov.cdc.ocio.processingnotifications.query.UploadMetricsQuery
import mu.KotlinLogging
import org.koin.core.component.inject


class NotificationActivitiesUploadDigestCountsImpl : NotificationActivitiesImpl() {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    override fun collectData(request: DataRequest): ResultWrapper<DataResponse> {
        val uploadDigestCountsRequest = request as UploadDigestCountsRequest

        // Upload digest query to get all the counts by data stream id, data stream route, and jurisdiction
        val uploadDigestQuery = UploadDigestCountsQuery.Builder(repository)
            .withDataStreamIds(uploadDigestCountsRequest.dataStreamIds)
            .withDataStreamRoutes(uploadDigestCountsRequest.dataStreamRoutes)
            .withJurisdictions(uploadDigestCountsRequest.jurisdictions)
            .withUtcToRun(uploadDigestCountsRequest.utcDateToRun)
            .build()
        val uploadDigestResults = uploadDigestQuery.run()

        // Aggregate the upload counts
        val aggregatedCounts = aggregateUploadCounts(uploadDigestResults)

        logger.info("Aggregated counts: ${aggregatedCounts.digest.size}")

        // Get the upload metrics
        val uploadMetricsQuery = UploadMetricsQuery.Builder(repository)
            .withDataStreamIds(uploadDigestCountsRequest.dataStreamIds)
            .withDataStreamRoutes(uploadDigestCountsRequest.dataStreamRoutes)
            .withJurisdictions(uploadDigestCountsRequest.jurisdictions)
            .withUtcToRun(uploadDigestCountsRequest.utcDateToRun)
            .build()
        val uploadMetrics = uploadMetricsQuery.run()

        // Get the upload and delivery durations
        val uploadDurationsQuery = UploadDurationQuery.Builder(repository)
            .withDataStreamIds(uploadDigestCountsRequest.dataStreamIds)
            .withDataStreamRoutes(uploadDigestCountsRequest.dataStreamRoutes)
            .withJurisdictions(uploadDigestCountsRequest.jurisdictions)
            .withUtcToRun(uploadDigestCountsRequest.utcDateToRun)
            .build()
        val uploadDurations = uploadDurationsQuery.run()

        return ResultWrapper.Success(UploadDigestCountsResponse(aggregatedCounts, uploadMetrics, uploadDurations))
    }

    /**
     *  DynamoDB does not support GROUP BY, so this function is used to aggregate the counts
     *  Function that groups by jurisdictionId and dataStreamId to aggregate counts.
     *
     *  @param uploadCounts List<UploadDigestResponse>
     *  @return UploadDigestCounts
     */
    private fun aggregateUploadCounts(
        uploadCounts: List<UploadDigestResponse>
    ): UploadDigestCounts {

        val digest = uploadCounts.groupBy { it.dataStreamId }
            .mapValues { (_, dataStreamCounts) ->
                dataStreamCounts.groupBy { it.dataStreamRoute }
                    .mapValues { (_, routeCounts) ->
                        routeCounts.groupBy { it.jurisdiction }
                            .mapValues { (_, jurisdictionCounts) ->
                                Counts(
                                    jurisdictionCounts.sumOf { it.started },
                                    jurisdictionCounts.sumOf { it.completed },
                                    jurisdictionCounts.sumOf { it.failedDelivery },
                                    jurisdictionCounts.sumOf { it.delivered }
                                )
                            }
                    }
            }
        return UploadDigestCounts(digest)
    }

}