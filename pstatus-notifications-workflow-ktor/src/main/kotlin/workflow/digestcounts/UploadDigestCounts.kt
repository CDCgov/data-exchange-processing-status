package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics


typealias CountsByJurisdiction = Map<String, Int>

typealias CountsByDataStreamRoute = Map<String, CountsByJurisdiction>

typealias CountsByDataStreamId = Map<String, CountsByDataStreamRoute>

data class TimingMetrics(
    val min: Float,
    val max: Float,
    val mean: Float,
    val median: Float
) {
    val stats = DescriptiveStatistics()

}

data class UploadDigestCounts(
    val digest: CountsByDataStreamId
)

