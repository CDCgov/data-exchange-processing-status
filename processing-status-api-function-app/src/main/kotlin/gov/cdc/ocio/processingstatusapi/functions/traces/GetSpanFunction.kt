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
        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")

        val uploadId = request.queryParameters["uploadId"]

        val stageName = request.queryParameters["stageName"]

        if (!uploadId.isNullOrBlank() && !stageName.isNullOrBlank()) {
            // Attempt to locate the trace by uploadId
            val traces = TraceUtils.getTraces(uploadId)
                ?: return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("The uploadId provided was not found.")
                    .build()

            if (traces.size != 1) {
                return request
                    .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Trace inconsistency found, expected exactly one trace for uploadId = $uploadId, but got ${traces.size}")
                    .build()
            }
            val traceResult = TraceResult.buildFromTrace(traces[0])

            // Found the trace by uploadId, now try to see if we can find at least one stage with a matching name.
            val latestMatchingSpan = traceResult.spans
                ?.filter { span -> span.stageName == stageName }
                ?.sortedBy { spanResult -> spanResult.timestamp } // natural sort order - oldest first to newest
                ?.lastOrNull() // get the last one, which will be the most recent timestamp

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

}