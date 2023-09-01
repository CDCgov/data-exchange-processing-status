package gov.cdc.ocio.processingstatusapi.functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.TraceResult
import java.util.*


class TraceFunction {

    private var tracing = TracingConfig.tracing

    fun run(
        request: HttpRequestMessage<Optional<String>>,
        providerName: String,
        context: ExecutionContext
    ): HttpResponseMessage {

        val logger = context.logger

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        logger.info("provider name = $providerName")

        tracing!!.tracer().startScopedSpan("doWork")
        val span = tracing!!.tracer().currentSpan()
        try {
            span.tag("key", "firstProcessingStage")
            Thread.sleep(500)
        } finally {
            span.finish()
        }

        val result = TraceResult()
        result.status = "OK"
        result.traceId = span.context().traceIdString()
        result.spanId = span.context().spanIdString()

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

}