package gov.cdc.ocio.processingstatusapi.model.traces

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.functions.status.GetStatusFunction
import java.time.Instant
import java.util.*

data class TraceResult(

    @SerializedName("trace_id")
    var traceId: String? = null,

    @SerializedName("span_id")
    var spanId: String? = null,

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("destination_id")
    var destinationId: String? = null,

    @SerializedName("event_type")
    var eventType: String? = null,

    var spans: List<SpanResult>? = null
) {
    companion object {

        /**
         * Provide the trace result from the trace model.
         *
         * @param traceModel Base
         * @return TraceResult
         */
        fun buildFromTrace(traceModel: Data): TraceResult {

            // Parent span will always be the first element in the array
            val uploadIdTag = traceModel.spans[0].tags.firstOrNull { it.key.equals("uploadId") }
            val destinationIdTag = traceModel.spans[0].tags.firstOrNull { it.key.equals("destinationId") }
            val eventTypeTag = traceModel.spans[0].tags.firstOrNull { it.key.equals("eventType") }

            // Remove the parent span since we'll be doing a foreach on all remaining spans
            traceModel.spans.removeAt(0)

            // Iterate through all the remaining spans and map them by operationName aka stageName
            val spanMap = mutableMapOf<String, List<Spans>>()
            traceModel.spans.forEach { span ->
                span.operationName?.let { stageName ->
                    var spansList = spanMap[stageName]?.toMutableList()
                    if (spansList == null)
                        spansList = mutableListOf()
                    spansList.add(span)
                    spanMap[stageName] = spansList
                }
            }

            // Build the span results associated with this trace
            val spanResults = mutableListOf<SpanResult>()
            spanMap.entries.forEach { entry ->
                val stageName = entry.key
                val spansList = entry.value

                // Determine whether the stage is running or completed.  If the start mark is found, but not the stop mark
                // then it is still running.
                var startTimestamp: Long? = null
                var stopTimestamp: Long? = null
                val tags = mutableListOf<Tags>()
                spansList.forEach { span ->
                    val spanMarkTag = span.tags.firstOrNull { it.key.equals("spanMark") }
                    when (spanMarkTag?.value) {
                        "start" -> startTimestamp = span.startTime?.div(1000) // microseconds to milliseconds
                        "stop"  -> stopTimestamp  = span.startTime?.div(1000) // microseconds to milliseconds
                    }
                    tags.addAll(span.tags.filterNot { GetStatusFunction.excludedSpanTags.contains(it.key) })
                }
                val isSpanComplete = (startTimestamp != null && stopTimestamp != null)

                val elapsedMillis = if (isSpanComplete) {
                    stopTimestamp!! - startTimestamp!!
                } else if (startTimestamp != null) {
                    Instant.now().toEpochMilli() - startTimestamp!!
                } else {
                    null
                }

                val spanResult = SpanResult().apply {
                    this.stageName = stageName
                    timestamp = startTimestamp?.let { Date(it) }
                    status = if (isSpanComplete) "complete" else "running"
                    this.elapsedMillis = elapsedMillis
                    metadata = tags
                }
                spanResults.add(spanResult)
            }

            val result = TraceResult().apply {
                this.traceId = traceModel.traceID
                spanId = traceModel.spans[0].spanID
                uploadId = uploadIdTag?.value
                destinationId = destinationIdTag?.value
                eventType = eventTypeTag?.value
                spans = spanResults
            }

            return result
        }
    }
}