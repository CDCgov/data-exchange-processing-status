package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.processingnotifications.activity.DataRequest
import gov.cdc.ocio.processingnotifications.activity.DataResponse
import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.activity.ResultWrapper
import gov.cdc.ocio.processingnotifications.service.ReportService


class TopErrorsNotificationActivitiesImpl : NotificationActivitiesImpl() {

    override fun collectData(request: DataRequest): ResultWrapper<DataResponse> {
        val uploadDigestCountsRequest = request as TopErrorsRequest

        val dataStreamId = uploadDigestCountsRequest.dataStreamId
        val dataStreamRoute = uploadDigestCountsRequest.dataStreamRoute
        val dayInterval = uploadDigestCountsRequest.dayInterval

        // Logic to determine the counts
        val reportService = ReportService()
        val failedMetadataVerifyCount = reportService.countFailedReports(dataStreamId, dataStreamRoute, StageAction.METADATA_VERIFY, dayInterval)
        val failedDeliveryCount = reportService.countFailedReports(dataStreamId, dataStreamRoute, StageAction.FILE_DELIVERY, dayInterval)
        val delayedUploads = reportService.getDelayedUploads(dataStreamId, dataStreamRoute, dayInterval)
        val delayedDeliveries = reportService.getDelayedDeliveries(dataStreamId, dataStreamRoute, dayInterval)
        val abandonedUploads = reportService.getAbandonedUploads(dataStreamId, dataStreamRoute)

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