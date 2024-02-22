package gov.cdc.ocio.processingstatusapi.functions.traces

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.opentelemetry.OpenTelemetryConfig
import gov.cdc.ocio.processingstatusapi.model.traces.TraceResult
import mu.KotlinLogging
import java.util.*


/**
 *  Creates a new distributed tracing trace for the given HTTP request
 */
class CreateTraceFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {
    private val logger = KotlinLogging.logger {}

    private val openTelemetry by lazy {
        OpenTelemetryConfig.initOpenTelemetry()
    }

    private val tracer = openTelemetry.getTracer(CreateTraceFunction::class.java.name)

    private val uploadId = request.queryParameters["uploadId"]

    private var destinationId = request.queryParameters["destinationId"]

    private val dataStreamId = request.queryParameters["dataStreamId"]

    private var eventType = request.queryParameters["eventType"]

    private val dataStreamRoute = request.queryParameters["dataStreamRoute"]

    /**
     * Creates a new distributed tracing trace for the given HTTP request.
     * In order to process, the HTTP request must contain uploadId
     *
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun create(): HttpResponseMessage {
        // Verify the request is complete and properly formatted
        checkRequired()?.let { return it }

        logger.info("uploadId: $uploadId")

        val span = tracer!!.spanBuilder(PARENT_SPAN).startSpan()
        span.setAttribute("uploadId", uploadId!!)
        span.setAttribute("destinationId", destinationId!!)
        span.setAttribute("eventType", eventType!!)
        span.end()

        val result = TraceResult().apply {
            traceId = span.spanContext.traceId
            spanId = span.spanContext.spanId
        }
        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

    /**
     * Creates a new distributed tracing trace for the given HTTP request.
     * In order to process, the HTTP request must contain uploadId
     *
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun createV2(): HttpResponseMessage {
        // Verify the request is complete and properly formatted
        checkRequired_v2()?.let { return it }

        logger.info("uploadId: $uploadId")

        val span = tracer!!.spanBuilder(PARENT_SPAN).startSpan()
        span.setAttribute("uploadId", uploadId!!)
        span.setAttribute("dataStreamId", dataStreamId!!)
        span.setAttribute("dataStreamRoute", dataStreamRoute!!)
        span.end()

        val result = TraceResult().apply {
            traceId = span.spanContext.traceId
            spanId = span.spanContext.spanId
        }
        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

    /**
     * Checks that all the required query parameters are present in order to process the request.  If not,
     * an appropriate HTTP response message is generated with the details.
     *
     * @return HttpResponseMessage?
     */
    private fun checkRequired(): HttpResponseMessage? {

        if (uploadId == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("uploadId is required")
                .build()
        }

        if (destinationId == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("destinationId is required")
                .build()
        }

        if (eventType == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("eventType is required")
                .build()
        }

        return null
    }

    /**
     * Checks that all the required query parameters are present in order to process the request.  If not,
     * an appropriate HTTP response message is generated with the details.
     *
     * @return HttpResponseMessage?
     */
    private fun checkRequired_v2(): HttpResponseMessage? {

        if (uploadId == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("uploadId is required")
                .build()
        }

        if (dataStreamId == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("dataStreamId is required")
                .build()
        }

        if (dataStreamRoute == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("dataStreamRoute is required")
                .build()
        }

        return null
    }

    companion object {
        private const val PARENT_SPAN = "PARENT_SPAN"
    }
}