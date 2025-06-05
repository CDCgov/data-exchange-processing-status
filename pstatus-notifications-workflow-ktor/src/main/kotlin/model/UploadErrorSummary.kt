package gov.cdc.ocio.processingnotifications.model

data class UploadErrorSummary(
    val failedMetadataVerifyCount: Int,
    val failedDeliveryCount: Int,
    val delayedUploads: List<UploadInfo>,
    val delayedDeliveries: List<UploadInfo>,
    val abandonedUploads: List<UploadInfo>
)
