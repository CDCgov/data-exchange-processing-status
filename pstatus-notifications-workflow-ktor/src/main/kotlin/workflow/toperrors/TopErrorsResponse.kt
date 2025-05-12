package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.processingnotifications.activity.DataResponse
import gov.cdc.ocio.processingnotifications.model.UploadInfo


data class TopErrorsResponse(
    val failedMetadataValidationCount: Int,
    val failedDeliveryCount: Int,
    val delayedUploads: List<UploadInfo>,
    val delayedDeliveries: List<UploadInfo>,
    val abandonedUploads: List<UploadInfo>,
) : DataResponse