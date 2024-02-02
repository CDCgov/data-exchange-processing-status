package gov.cdc.ocio.processingstatusapi.functions.traces

import com.google.gson.Gson
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.model.traces.Tags
import gov.cdc.ocio.processingstatusapi.model.traces.TraceResult
import gov.cdc.ocio.processingstatusapi.opentelemetry.OpenTelemetryConfig
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import mu.KotlinLogging
import java.util.*


/**
 * Create a processing status span for a given trace.
 */
class AddSpanToTraceFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {
    private val logger = KotlinLogging.logger {}

    private val openTelemetry by lazy {
        OpenTelemetryConfig.initOpenTelemetry()
    }

    private val tracer = openTelemetry.getTracer(AddSpanToTraceFunction::class.java.name)

    private val requestBody = request.body.orElse("")

    /**
     * For a given HTTP request, this method creates a processing status span for a given trace.
     * In order to process, the HTTP request must contain stageName and spanMark.
     *
     * @param traceId String
     * @param parentSpanId String parent span ID
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun startSpan(
        traceId: String,
        parentSpanId: String
    ): HttpResponseMessage {

        val stageName = request.queryParameters["stageName"]
            ?: return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("stageName is required")
                .build()

        logger.info("stageName: $stageName")

        val result: TraceResult
        try {
            // See if we were given any optional tags
            var tags: Array<Tags>? = null
            try {
                if (requestBody.isNotBlank()) {
                    tags = Gson().fromJson(requestBody, Array<Tags>::class.java)
                }
            } catch (e: Exception) {
                throw BadRequestException("Failed to parse the optional metadata tags in the request body")
            }

            val spanContext = SpanContext.createFromRemoteParent(
                traceId,
                parentSpanId,
                TraceFlags.getSampled(),
                TraceState.getDefault()
            )

            val span = tracer!!.spanBuilder(stageName)
                .setParent(Context.current().with(Span.wrap(spanContext)))
                .startSpan()

            tags?.forEach {
                if (it.key != null && it.value != null)
                    span.setAttribute(it.key!!, it.value!!)
            }
            span.end()

            result = TraceResult().apply {
                this.traceId = span.spanContext.traceId
                this.spanId = span.spanContext.spanId
            }

        } catch (ex: BadRequestException) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(ex.localizedMessage)
                .build()
        }

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

    /**
     * For a given HTTP request, this method creates a processing status span for a given trace.
     * In order to process, the HTTP request must contain stageName and spanMark.
     *
     * @param traceId String
     * @param spanId String
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     * @throws BadRequestException
     * @throws BadStateException
     */
    fun stopSpan(
        traceId: String,
        spanId: String
    ): HttpResponseMessage {


        try {
           val spanContext = SpanContext.createFromRemoteParent(
                traceId,
                spanId,
                TraceFlags.getSampled(),
                TraceState.getDefault()
            )
            val span = tracer!!.spanBuilder(spanId)
                .setParent(Context.current().with(Span.wrap(spanContext)))
                .startSpan()
            span.end()

        } catch (ex: BadRequestException) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(ex.localizedMessage)
                .build()
        }

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .build()
    }

}