package gov.cdc.ocio.processingstatusapi.functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.TraceResult
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import java.util.*


class TraceFunction {

    private var tracer: Tracer? = null

    fun run(
        request: HttpRequestMessage<Optional<String>>,
        spanName: String,
        context: ExecutionContext
    ): HttpResponseMessage {

        val logger = context.logger

        //val jaegerEndpoint = "https://ocio-ede-dev-jaeger-app-service.azurewebsites.net:4317"
        val jaegerEndpoint = "http://ocioededevjaeger.eastus.azurecontainer.io:4317"
        val openTelemetry = OpenTelemetryConfig.initOpenTelemetry(jaegerEndpoint)

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        logger.info("span name = $spanName")

//        val someParam = request.queryParameters["someParam"]

        tracer = openTelemetry.getTracer(TraceFunction::class.java.name)

        val span: Span = tracer!!.spanBuilder(spanName).startSpan()
        try {
            span.setAttribute("some_field", UUID.randomUUID().toString())
            span.makeCurrent().use { ignored ->
                Thread.sleep(500)
                logger.info("A sample log message!")
            }
        } finally {
            span.end()
        }

        val result = TraceResult()
        result.status = "OK"
        result.traceId = span.spanContext.traceId
        result.spanId = span.spanContext.spanId

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

}