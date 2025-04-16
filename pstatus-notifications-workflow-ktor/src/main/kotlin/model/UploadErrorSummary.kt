package gov.cdc.ocio.processingnotifications.model

data class UploadErrorSummary(
    val metadata: MetadataGroup,
    val failedMetadataVerifyCount: Int,
    val failedDeliveryCount: Int,
    val delayedUploads: List<String>,
    val delayedDeliveries: List<String>,
    val sinceDays: Int,
)