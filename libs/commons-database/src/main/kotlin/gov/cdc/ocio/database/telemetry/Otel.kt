package gov.cdc.ocio.database.telemetry

import io.opentelemetry.sdk.metrics.Aggregation
import io.opentelemetry.sdk.metrics.View

object Otel {
    /**
     * Returns a more appropriate list of buckets for OpenTelemetry histograms.  Each bucket is in nanoseconds.
     */
    fun getDefaultHistogramView(): View {
        return View.builder().setAggregation(
            Aggregation.explicitBucketHistogram(
                listOf(100000.0,
                    250000.0,
                    500000.0,
                    1000000.0,
                    10000000.0,
                    100000000.0,
                    1000000000.0,
                    10000000000.0)
            )).build()
    }
}