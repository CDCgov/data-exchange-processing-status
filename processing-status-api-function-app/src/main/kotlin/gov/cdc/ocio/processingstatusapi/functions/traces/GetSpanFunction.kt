package gov.cdc.ocio.processingstatusapi.functions.traces

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.traces.*
import gov.cdc.ocio.processingstatusapi.utils.JsonUtils
import mu.KotlinLogging
import java.util.*

/**
 * Collection of functions to get traces.
 */
class GetSpanFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {
    private val logger = KotlinLogging.logger {}

    private val gson = JsonUtils.getGsonBuilderWithUTC()

    fun withQueryParams(): HttpResponseMessage {

        val uploadId = request.queryParameters["uploadId"]

        val stageName = request.queryParameters["stageName"]

        if (!uploadId.isNullOrBlank() && !stageName.isNullOrBlank()) {

            var latestMatchingSpan: SpanResult?
            if (TraceUtils.tracingEnabled) {
                var attempts = 0
                var traces: List<Data>
                do {
                    // Attempt to locate the trace by uploadId
                    traces = TraceUtils.getTraces(uploadId)
                        ?: return request
                            .createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body("The uploadId provided was not found.")
                            .build()

                    latestMatchingSpan = checkForStageNameInTraces(traces, stageName)
                    if (latestMatchingSpan != null) {
                        break
                    }
                    Thread.sleep(500)
                } while (attempts++ < 20) // try for up to 10 seconds

                if (traces.size != 1) {
                    return request
                        .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Trace inconsistency found, expected exactly one trace for uploadId = $uploadId, but got ${traces.size}")
                        .build()
                }
            } else {
                latestMatchingSpan = SpanResult().apply {
                    this.spanId = TraceUtils.TRACING_DISABLED
                    this.traceId = TraceUtils.TRACING_DISABLED
                    this.stageName = stageName
                    this.timestamp = Date()
                }
            }

            return if (latestMatchingSpan != null) {
                // Found a match, return it
                request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(latestMatchingSpan))
                    .build()
            } else {
                request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Found the uploadId provided, but not the stageName")
                    .build()
            }
        }

        return request
            .createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("Unrecognized combination of query parameters provided")
            .build()
    }

    /**
     * Check for a span in the given trace with stage name provide.
     *
     * @param traces List<Data>
     * @param stageName String
     * @return SpanResult?
     */
    private fun checkForStageNameInTraces(traces: List<Data>, stageName: String): SpanResult? {
        if (traces.size != 1)
            return null

        val traceResult = TraceResult.buildFromTrace(traces[0])

        // Found the trace by uploadId, now try to see if we can find at least one stage with a matching name.
        val latestMatchingSpan = traceResult.spans
            ?.filter { span -> span.stageName == stageName }
            ?.sortedBy { spanResult -> spanResult.timestamp } // natural sort order - oldest first to newest
            ?.lastOrNull() // get the last one, which will be the most recent timestamp

        if (latestMatchingSpan != null) {
            // Found a match, return it
            return latestMatchingSpan
        }

        return null
    }

}