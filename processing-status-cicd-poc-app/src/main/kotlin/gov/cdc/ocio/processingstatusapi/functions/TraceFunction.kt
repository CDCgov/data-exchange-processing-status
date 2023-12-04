package gov.cdc.ocio.processingstatusapi.functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.Constants
import gov.cdc.ocio.processingstatusapi.model.TraceResult
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import java.util.*


/**
 *  Creates a new distributed tracing trace for the given HTTP request
 */
class TraceFunction {

    private var tracer: Tracer? = null

    /**
     * Creates a new distributed tracing trace for the given HTTP request.
     * In order to process, the HTTP request must contain stageName
     *
     * @param request HttpRequestMessage<Optional<String>>
     * @param context ExecutionContext
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun run(
        request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext
    ): HttpResponseMessage {

        val logger = context.logger
        val openTelemetry = OpenTelemetryConfig.initOpenTelemetry()
        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        val stageName = request.queryParameters["stageName"]
        logger.info("StageName: $stageName")

        val result = TraceResult()
        if(stageName!= null) {
            tracer = openTelemetry.getTracer(TraceFunction::class.java.name)

            val span: Span = tracer!!.spanBuilder(Constants.PARENT_SPAN).startSpan()
            span.setAttribute("stageName", stageName.uppercase())
            span.end()

            result.traceId = span.spanContext.traceId
            result.spanId = span.spanContext.spanId
            return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(result)
                .build()
        } else {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")
                .body("The provided stageName is not allowed.")
                .build()
        }
    }
}