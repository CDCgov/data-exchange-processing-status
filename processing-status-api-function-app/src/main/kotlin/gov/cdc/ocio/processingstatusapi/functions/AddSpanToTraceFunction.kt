package gov.cdc.ocio.processingstatusapi.functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.TraceResult
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import java.util.*


/**
 * Create a processing status span for a given trace
 *
 */
class AddSpanToTraceFunction {

    private var tracer: Tracer? = null

    /**
     * For a given HTTP request, this method creates a processing status span for a given trace.
     * In order to process, the HTTP request must contain stageName and spanMark.
     * @param request HttpRequestMessage<Optional<String>>
     * @param traceId String
     * @param spanId String
     * @param context ExecutionContext
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun run(
        request: HttpRequestMessage<Optional<String>>,
        traceId: String,
        spanId: String,
        context: ExecutionContext
    ): HttpResponseMessage {

        val logger = context.logger
        val openTelemetry = OpenTelemetryConfig.initOpenTelemetry()

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        val stageName = request.queryParameters["stageName"]
        val spanMark = request.queryParameters["spanMark"]
        logger.info("StageName: $stageName")
        logger.info("SpanMark: $spanMark")
        logger.info(spanMark)
        tracer = openTelemetry.getTracer(AddSpanToTraceFunction::class.java.name)

        val spanContext = SpanContext.createFromRemoteParent(
            traceId,
            spanId,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        )

        if(spanMark!= null && (spanMark == "start" || spanMark == "stop")) {
            tracer = openTelemetry.getTracer(TraceFunction::class.java.name)

            val span = tracer!!.spanBuilder(stageName)
                .setParent(Context.current().with(Span.wrap(spanContext)))
                .startSpan()
            span.setAttribute("spanMark", spanMark)
            span.end()

            val result = TraceResult()
            result.traceId = traceId
            result.spanId = spanId
            return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(result)
                .build()

        } else{
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")
                .body("The provided spanMark is not allowed.")
                .build()
        }

    }

}