package gov.cdc.ocio.processingnotifications.workflow.digestcounts


/**
 * Timing and file metrics for an upload and delivery of that upload.
 *
 * @property minUploadDeltaInMillis Long
 * @property maxUploadDeltaInMillis Long
 * @property meanUploadDeltaInMillis Float
 * @property medianUploadDeltaInMillis Float
 * @property minDeliveryDeltaInMillis Long
 * @property maxDeliveryDeltaInMillis Long
 * @property meanDeliveryDeltaInMillis Float
 * @property medianDeliveryDeltaInMillis Float
 * @property minFileSize Long
 * @property maxFileSize Long
 * @property meanFileSize Float
 * @property medianFileSize Float
 * @constructor
 */
data class UploadMetrics(
    val minUploadDeltaInMillis: Long,
    val maxUploadDeltaInMillis: Long,
    val meanUploadDeltaInMillis: Float,
    val medianUploadDeltaInMillis: Float,
    val minDeliveryDeltaInMillis: Long,
    val maxDeliveryDeltaInMillis: Long,
    val meanDeliveryDeltaInMillis: Float,
    val medianDeliveryDeltaInMillis: Float,
    val minFileSize: Long,
    val maxFileSize: Long,
    val meanFileSize: Float,
    val medianFileSize: Float
)