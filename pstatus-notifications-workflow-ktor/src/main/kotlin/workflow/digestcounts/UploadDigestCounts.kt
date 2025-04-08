package gov.cdc.ocio.processingnotifications.workflow.digestcounts


typealias CountsByJurisdiction = Map<String, Int>

typealias CountsByDataStreamRoute = Map<String, CountsByJurisdiction>

typealias CountsByDataStreamId = Map<String, CountsByDataStreamRoute>

data class TimingMetrics(
    val minDelta: Float,
    val maxDelta: Float,
    val meanDelta: Float,
    val medianDelta: Float
)

data class UploadDigestCounts(
    val digest: CountsByDataStreamId
)

