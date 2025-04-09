package gov.cdc.ocio.processingnotifications.workflow.digestcounts

data class UploadMetrics(
    val minDelta: Long,
    val maxDelta: Long,
    val meanDelta: Float,
    val medianDelta: Float,
    val minFileSize: Long,
    val maxFileSize: Long,
    val meanFileSize: Float,
    val medianFileSize: Float
)