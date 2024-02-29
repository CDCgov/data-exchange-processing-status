package gov.cdc.ocio.processingstatusapi.functions.traces

import com.google.gson.*
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
class GetTraceFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {
    private val logger = KotlinLogging.logger {}

    private val gson = JsonUtils.getGsonBuilderWithUTC()

    /**
     * For a given HTTP request, this method fetches trace information for a given traceId.
     *
     * @param traceId String
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun withTraceId(traceId: String): HttpResponseMessage {

        logger.info("Trace Id = $traceId")

        var traceResult: TraceResult? = null
        if (TraceUtils.tracingEnabled) {
            val traceEndPoint = System.getenv("JAEGER_TRACE_END_POINT") + "api/traces/$traceId"
            logger.info("traceEndPoint: $traceEndPoint")
            var attempts = 0
            do {
                val response = khttp.get(traceEndPoint)
                val obj = response.jsonObject
                logger.info("$obj")

                if (response.statusCode == HttpStatus.OK.value()) {
                    val traceModel = Gson().fromJson(obj.toString(), Base::class.java)
                    traceResult = TraceResult.buildFromTrace(traceModel.data[0])
                    break
                }
                Thread.sleep(500)
            } while (attempts++ < 20) // try for up to 10 seconds
        } else {
            traceResult = disabledTraceResult
        }

        if (traceResult != null) {
            return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(gson.toJson(traceResult))
                .build()
        }

        return request
            .createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("The trace identifier provided was not found.")
            .build()
    }

    /**
     * For a given HTTP request, this method fetches trace information for a given uploadId.
     *
     * @param uploadId String
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun withUploadId(uploadId: String): HttpResponseMessage {

        logger.info("Upload Id = $uploadId")

        var traceResult: TraceResult? = null
        if (TraceUtils.tracingEnabled) {
            var attempts = 0
            do {
                val traces = TraceUtils.getTraces(uploadId)
                    ?: return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("The uploadId provided was not found.")
                        .build()

                if (traces.size == 1) {
                    traceResult = TraceResult.buildFromTrace(traces[0])
                    break
                }

                Thread.sleep(500)
            } while (attempts++ < 20) // try for up to 10 seconds
        } else {
            traceResult = disabledTraceResult
        }

        if (traceResult != null) {
            return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(gson.toJson(traceResult))
                .build()
        }

        return request
            .createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("The upload identifier provided was not found.")
            .build()
    }

    companion object {
        private val disabledTraceResult = TraceResult().apply {
            this.traceId = TraceUtils.TRACING_DISABLED
            this.destinationId = TraceUtils.TRACING_DISABLED
            this.spanId = TraceUtils.TRACING_DISABLED
            this.eventType = TraceUtils.TRACING_DISABLED
            this.uploadId = TraceUtils.TRACING_DISABLED
        }
    }

}