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
            val parentSpanId = traceModel.spans[0].spanID

            // Remove the parent span since we'll be doing a foreach on all remaining spans
            traceModel.spans.removeAt(0)

            // Iterate through all the remaining spans and map them by operationName aka stageName
            val spanMap = mutableMapOf<String, List<Spans>>()
            val parentSpans = traceModel.spans.filter { span -> span.references.any { ref -> ref.spanID == parentSpanId } }
            parentSpans.forEach { span ->
                span.operationName?.let { stageName ->
                    var spansList = spanMap[stageName]?.toMutableList()
                    if (spansList == null)
                        spansList = mutableListOf()
                    val tag = Tags().apply {
                        key = "spanMark"
                        value = "start"
                    }
                    span.tags.add(tag)
                    spansList.add(span)
                    spanMap[stageName] = spansList
                }
            }
            val childSpans = traceModel.spans.filter { span -> span.references.any { ref -> ref.refType == "CHILD_OF" } }
            childSpans.forEach { span ->
                // Find the parent span
                val matchParentSpanId = span.references.firstOrNull { ref -> ref.refType == "CHILD_OF" }?.spanID
                val match = spanMap.values.firstOrNull { spanList -> spanList.any { span -> span.spanID == matchParentSpanId } }
                val stageName = match?.get(0)?.operationName
                stageName?.let {
                    var stageSpansList = spanMap[stageName]?.toMutableList()
                    if (stageSpansList == null)
                        stageSpansList = mutableListOf()
                    val tag = Tags().apply {
                        key = "spanMark"
                        value = "stop"
                    }
                    span.tags.add(tag)
                    stageSpansList.add(span)
                    spanMap[stageName] = stageSpansList
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
                spanId = parentSpanId
                uploadId = uploadIdTag?.value
                destinationId = destinationIdTag?.value
                eventType = eventTypeTag?.value
                spans = spanResults
            }

            return result
        }
    }
}