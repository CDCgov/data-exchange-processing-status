package gov.cdc.ocio.processingstatusapi.functions.traces

import com.google.gson.*
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.traces.*
import gov.cdc.ocio.processingstatusapi.utils.JsonUtils
import java.time.Instant
import java.util.*

/**
 * Collection of functions to get traces.
 */
class GetTraceFunction(
    private val request: HttpRequestMessage<Optional<String>>,
    context: ExecutionContext
) {
    private val logger = context.logger

    private val gson = JsonUtils.getGsonBuilderWithUTC()

    /**
     * For a given HTTP request, this method fetches trace information for a given traceId.
     *
     * @param traceId String
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun withTraceId(traceId: String): HttpResponseMessage {

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        logger.info("Trace Id = $traceId")

        val traceEndPoint = System.getenv("JAEGER_TRACE_END_POINT")+"api/traces/$traceId"
        logger.info("traceEndPoint: $traceEndPoint")
        val response =  khttp.get(traceEndPoint)
        val obj = response.jsonObject
        logger.info("$obj")

        if (response.statusCode != HttpStatus.OK.value()) {
            return request
                .createResponseBuilder(HttpStatus.valueOf(response.statusCode))
                .body("Bad request. The identifier provided was not found.")
                .build()
        }

        val traceModel = Gson().fromJson(obj.toString(), Base::class.java)
        val result = resultFromTrace(traceModel)

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(gson.toJson(result))
            .build()
    }

    /**
     * For a given HTTP request, this method fetches trace information for a given uploadId.
     *
     * @param uploadId String
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun withUploadId(uploadId: String): HttpResponseMessage {

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        logger.info("Upload Id = $uploadId")

        val traceEndPoint = System.getenv("JAEGER_TRACE_END_POINT")+"api/traces/$uploadId"
        logger.info("traceEndPoint: $traceEndPoint")
        val response = khttp.get(traceEndPoint)
        val obj = response.jsonObject
        logger.info("$obj")

        if (response.statusCode != HttpStatus.OK.value()) {
            return request
                .createResponseBuilder(HttpStatus.valueOf(response.statusCode))
                .header("Content-Type", "application/json")
                .body("Bad request. The identifier provided was not found.")
                .build()
        }

        val traceModel = Gson().fromJson(obj.toString(), Base::class.java)
        val result = resultFromTrace(traceModel)

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(gson.toJson(result))
            .build()
    }

    /**
     * Provide the trace result from the trace model.
     *
     * @param traceModel Base
     * @return TraceResult
     */
    private fun resultFromTrace(traceModel: Base): TraceResult {

        // Parent span will always be the first element in the array
        val uploadIdTag = traceModel.data[0].spans[0].tags.firstOrNull { it.key.equals("uploadId") }
        val destinationIdTag = traceModel.data[0].spans[0].tags.firstOrNull { it.key.equals("destinationId") }
        val eventTypeTag = traceModel.data[0].spans[0].tags.firstOrNull { it.key.equals("eventType") }

        // Remove the parent span since we'll be doing a foreach on all remaining spans
        traceModel.data[0].spans.removeAt(0)

        // Iterate through all the remaining spans and map them by operationName aka stageName
        val spanMap = mutableMapOf<String, List<Spans>>()
        traceModel.data[0].spans.forEach { span ->
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
                tags.addAll(span.tags.filterNot { excludedSpanTags.contains(it.key) })
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
            this.traceId = traceModel.data[0].traceID
            spanId = traceModel.data[0].spans[0].spanID
            uploadId = uploadIdTag?.value
            destinationId = destinationIdTag?.value
            eventType = eventTypeTag?.value
            spans = spanResults
        }

        return result
    }

    companion object {
        val excludedSpanTags = listOf("spanMark", "span.kind", "internal.span.format")
    }
}