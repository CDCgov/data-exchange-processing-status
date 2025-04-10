package gov.cdc.ocio.processingnotifications.workflow.digestcounts


/**
 * Metrics for an upload.
 *
 * @property minDelta Long
 * @property maxDelta Long
 * @property meanDelta Float
 * @property medianDelta Float
 * @property minFileSize Long
 * @property maxFileSize Long
 * @property meanFileSize Float
 * @property medianFileSize Float
 * @constructor
 */
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