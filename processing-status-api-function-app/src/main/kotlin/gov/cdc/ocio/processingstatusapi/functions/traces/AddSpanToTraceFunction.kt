package gov.cdc.ocio.processingstatusapi.functions.traces

import com.google.gson.Gson
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.traces.SpanMarkType
import gov.cdc.ocio.processingstatusapi.model.traces.Tags
import gov.cdc.ocio.processingstatusapi.model.traces.TraceResult
import gov.cdc.ocio.processingstatusapi.opentelemetry.OpenTelemetryConfig
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import java.util.*


/**
 * Create a processing status span for a given trace.
 */
class AddSpanToTraceFunction(
    private val request: HttpRequestMessage<Optional<String>>,
    context: ExecutionContext
) {
    private val logger = context.logger

    private val openTelemetry by lazy {
        OpenTelemetryConfig.initOpenTelemetry()
    }

    private val tracer = openTelemetry.getTracer(AddSpanToTraceFunction::class.java.name)

    private val stageName = request.queryParameters["stageName"]

    private val spanMark = request.queryParameters["spanMark"]

    private var spanMarkType = SpanMarkType.UNKNOWN

    private val requestBody = request.body.orElse("")

    /**
     * For a given HTTP request, this method creates a processing status span for a given trace.
     * In order to process, the HTTP request must contain stageName and spanMark.
     *
     * @param traceId String
     * @param spanId String
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun addSpan(
        traceId: String,
        spanId: String
    ): HttpResponseMessage {

        // Verify the request is complete and properly formatted
        checkRequired()?.let { return it }

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")

        logger.info("stageName: $stageName")
        logger.info("spanMark: $spanMark")

        // See if we were given any optional tags
        var tags: Array<Tags>? = null
        try {
            if (requestBody.isNotBlank()) {
                tags = Gson().fromJson(requestBody, Array<Tags>::class.java)
            }
        } catch (e: Exception) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Failed to parse the optional metadata tags in the request body")
                .build()
        }

        val spanContext = SpanContext.createFromRemoteParent(
            traceId,
            spanId,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        )

        val span = tracer!!.spanBuilder(stageName!!)
            .setParent(Context.current().with(Span.wrap(spanContext)))
            .startSpan()
        span.setAttribute("spanMark", spanMark!!)
        tags?.forEach {
            if (it.key != null && it.value != null)
                span.setAttribute(it.key!!, it.value!!)
        }
        span.end()

        val result = TraceResult().apply {
            this.traceId = traceId
            this.spanId = spanId
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

        if (stageName == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("stageName is required")
                .build()
        }

        if (spanMark == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("spanMark is required")
                .build()
        }

        spanMarkType = when (spanMark.lowercase(Locale.getDefault())) {
            "start" -> SpanMarkType.START
            "stop" -> SpanMarkType.STOP
            else -> SpanMarkType.UNKNOWN
        }

        if (spanMarkType == SpanMarkType.UNKNOWN) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("spanMark is not a recognized value")
                .build()
        }

        return null
    }

}