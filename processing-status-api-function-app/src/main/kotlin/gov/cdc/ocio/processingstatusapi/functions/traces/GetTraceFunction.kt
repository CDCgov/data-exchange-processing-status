package gov.cdc.ocio.processingstatusapi.functions.traces

import com.google.gson.Gson
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.traces.Base
import gov.cdc.ocio.processingstatusapi.model.traces.TraceResult
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
            .body(result)
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
            .body(result)
            .build()
    }

    /**
     * Provide the trace result from the trace model.
     *
     * @param traceModel Base
     * @return TraceResult
     */
    private fun resultFromTrace(traceModel: Base): TraceResult {

        val uploadIdTag = traceModel.data[0].spans[0].tags.firstOrNull { it.key.equals("uploadId") }
        val destinationIdTag = traceModel.data[0].spans[0].tags.firstOrNull { it.key.equals("destinationId") }
        val eventTypeTag = traceModel.data[0].spans[0].tags.firstOrNull { it.key.equals("eventType") }

        // Determine whether the stage is running or completed.  If the start mark is found, but not the stop mark then
        // it is still running.
        var startTimestamp: Long? = null
        var stopTimestamp: Long? = null
        traceModel.data[0].spans.forEach { span ->
            val spanMarkTag = span.tags.firstOrNull { it.key.equals("spanMark") }
            when (spanMarkTag?.value) {
                "start" -> startTimestamp = span.startTime
                "stop" -> stopTimestamp = span.startTime
            }
        }
        val isSpanComplete = (startTimestamp != null && stopTimestamp != null)

        val elapsedMillis = if (isSpanComplete) {
            (stopTimestamp!! - startTimestamp!!) / 1000 // microseconds to milliseconds
        } else if (startTimestamp != null) {
            Instant.now().toEpochMilli() - (startTimestamp!! / 1000)
        } else {
            null
        }

        val result = TraceResult().apply {
            this.traceId = traceModel.data[0].traceID
            spanId = traceModel.data[0].spans[0].spanID
            uploadId = uploadIdTag?.value
            status = if (isSpanComplete) "complete" else "running"
            this.elapsedMillis = elapsedMillis
            this.timestamp = startTimestamp?.let { Date(it / 1000) }
            destinationId = destinationIdTag?.value
            eventType = eventTypeTag?.value
            metadata = traceModel.data[0].spans[0].tags.filter { it.key.equals("stageName") }
        }
        return result
    }

}