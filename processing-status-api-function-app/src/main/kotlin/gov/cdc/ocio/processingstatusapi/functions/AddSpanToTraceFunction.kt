package gov.cdc.ocio.processingstatusapi.functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.TraceResult
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import java.util.*


class AddSpanToTraceFunction {

    private var tracer: Tracer? = null

    fun run(
        request: HttpRequestMessage<Optional<String>>,
        traceId: String,
        spanId: String,
        providerName: String,
        context: ExecutionContext
    ): HttpResponseMessage {

        val logger = context.logger

        val jaegerEndpoint = "http://ocioededevjaeger.eastus.azurecontainer.io:4317"
        val openTelemetry = OpenTelemetryConfig.initOpenTelemetry(jaegerEndpoint)

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        logger.info("provider name = $providerName")

        tracer = openTelemetry.getTracer(AddSpanToTraceFunction::class.java.name)

        val spanContext = SpanContext.createFromRemoteParent(
            traceId,
            spanId,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        )

        val span = tracer!!.spanBuilder("stage2")
            .setParent(Context.current().with(Span.wrap(spanContext)))
            .startSpan()
        try {
            span.setAttribute("stage2_field1", providerName)
            span.makeCurrent().use { ignored ->
                Thread.sleep(500)
            }
        } finally {
            span.end()
        }

        val result = TraceResult()
        result.status = "OK"
        result.traceId = span.spanContext.traceId

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

}