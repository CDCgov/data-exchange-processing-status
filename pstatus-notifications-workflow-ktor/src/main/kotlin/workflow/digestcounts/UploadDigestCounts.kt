package gov.cdc.ocio.processingnotifications.workflow.digestcounts


data class Counts(
    val started: Int,
    val completed: Int,
    val failedDelivery: Int,
    val delivered: Int
)

typealias CountsByJurisdiction = Map<String, Counts>

typealias CountsByDataStreamRoute = Map<String, CountsByJurisdiction>

typealias CountsByDataStreamId = Map<String, CountsByDataStreamRoute>

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

data class UploadDigestCounts(
    val digest: CountsByDataStreamId
)

