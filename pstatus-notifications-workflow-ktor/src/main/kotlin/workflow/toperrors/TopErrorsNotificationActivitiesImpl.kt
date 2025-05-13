package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.processingnotifications.activity.DataRequest
import gov.cdc.ocio.processingnotifications.activity.DataResponse
import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.activity.ResultWrapper
import gov.cdc.ocio.processingnotifications.service.ReportService


/**
 * Implementation of the `NotificationActivitiesImpl` class that is responsible for
 * collecting and processing data related to top error notifications for reports.
 *
 * This class interacts with `ReportService` to gather error counts and report details
 * such as failed metadata verification, failed deliveries, delayed uploads, delayed
 * deliveries, and abandoned uploads. The collected data is wrapped into a `TopErrorsResponse`
 * and returned as a result.
 */
class TopErrorsNotificationActivitiesImpl : NotificationActivitiesImpl() {

    /**
     * Collects data regarding the most significant errors and issues within a specified
     * time interval for a given data stream. This includes metrics like failed metadata verification,
     * failed deliveries, delayed uploads, delayed deliveries, and abandoned uploads.
     *
     * @param request The data request of type [DataRequest].
     *        It must be an instance of [TopErrorsRequest] containing the data stream ID,
     *        route, and time interval for analysis.
     * @return A [ResultWrapper] wrapping a [TopErrorsResponse] containing the error metrics
     *         and insights for the given data stream and interval.
     */
    override fun collectData(request: DataRequest): ResultWrapper<DataResponse> {
        val uploadDigestCountsRequest = request as TopErrorsRequest

        val dataStreamId = uploadDigestCountsRequest.dataStreamId
        val dataStreamRoute = uploadDigestCountsRequest.dataStreamRoute
        val dayInterval = uploadDigestCountsRequest.dayInterval

        // ReportService must be instantiated here otherwise "java.lang.IllegalStateException: KoinApplication has not been started"
        // will be thrown during startup.
        val reportService = ReportService()

        // Logic to determine the counts
        val failedMetadataVerifyCount = reportService.countFailedReports(
            dataStreamId,
            dataStreamRoute,
            StageAction.METADATA_VERIFY,
            dayInterval
        )
        val failedDeliveryCount = reportService.countFailedReports(
            dataStreamId,
            dataStreamRoute,
            StageAction.FILE_DELIVERY,
            dayInterval
        )
        val delayedUploads = reportService.getDelayedUploads(
            dataStreamId,
            dataStreamRoute,
            dayInterval
        )
        val delayedDeliveries = reportService.getDelayedDeliveries(
            dataStreamId,
            dataStreamRoute,
            dayInterval
        )
        val abandonedUploads = reportService.getAbandonedUploads(
            dataStreamId,
            dataStreamRoute
        )

        return ResultWrapper.Success(
            TopErrorsResponse(
                failedMetadataVerifyCount,
                failedDeliveryCount,
                delayedUploads,
                delayedDeliveries,
                abandonedUploads
            )
        )
    }

}