package gov.cdc.ocio.processingstatusapi.model.traces

import com.google.gson.annotations.SerializedName

data class TraceDao(

    @SerializedName("trace_id")
    var traceId: String? = null,

    @SerializedName("span_id")
    var spanId: String? = null,

    var spans: List<SpanResult>? = null
) {
    companion object {

        /**
         * Provide the trace DAO from the trace result.
         *
         * @param traceResult TraceResult
         * @return TraceResult
         */
        fun buildFromTraceResult(traceResult: TraceResult): TraceDao {
            return TraceDao().apply {
                this.traceId = traceResult.traceId
                this.spanId = traceResult.spanId
                this.spans = traceResult.spans
            }
        }

        fun buildFromTraceResult(traceResult: TraceResultV2): TraceDao {
            return TraceDao().apply {
                this.traceId = traceResult.traceId
                this.spanId = traceResult.spanId
                this.spans = traceResult.spans
            }
        }
    }
}