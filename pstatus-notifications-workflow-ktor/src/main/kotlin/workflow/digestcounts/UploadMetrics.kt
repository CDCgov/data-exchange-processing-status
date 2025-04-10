package gov.cdc.ocio.processingnotifications.workflow.digestcounts


/**
 * Metrics for an upload.
 *
 * @property minDeltaInMillis Long
 * @property maxDeltaInMillis Long
 * @property meanDeltaInMillis Float
 * @property medianDeltaInMillis Float
 * @property minFileSize Long
 * @property maxFileSize Long
 * @property meanFileSize Float
 * @property medianFileSize Float
 * @constructor
 */
data class UploadMetrics(
    val minDeltaInMillis: Long,
    val maxDeltaInMillis: Long,
    val meanDeltaInMillis: Float,
    val medianDeltaInMillis: Float,
    val minFileSize: Long,
    val maxFileSize: Long,
    val meanFileSize: Float,
    val medianFileSize: Float
)