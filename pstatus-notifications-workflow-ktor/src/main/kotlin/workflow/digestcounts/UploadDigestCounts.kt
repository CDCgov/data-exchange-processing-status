package gov.cdc.ocio.processingnotifications.workflow.digestcounts


typealias CountsByJurisdiction = Map<String, Int>

typealias CountsByDataStreamRoute = Map<String, CountsByJurisdiction>

typealias CountsByDataStreamId = Map<String, CountsByDataStreamRoute>

data class TimingMetrics(
    val min: Float,
    val max: Float,
    val mean: Float,
    val median: Float
)

data class UploadDigestCounts(
    val digest: CountsByDataStreamId
)

