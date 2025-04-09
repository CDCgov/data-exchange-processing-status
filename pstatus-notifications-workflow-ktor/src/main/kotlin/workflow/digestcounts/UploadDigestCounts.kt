package gov.cdc.ocio.processingnotifications.workflow.digestcounts


typealias CountsByJurisdiction = Map<String, Int>

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

