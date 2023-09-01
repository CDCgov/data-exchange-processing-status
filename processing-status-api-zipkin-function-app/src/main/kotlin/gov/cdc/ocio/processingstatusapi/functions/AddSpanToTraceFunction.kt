package gov.cdc.ocio.processingstatusapi.functions

import brave.propagation.B3SingleFormat
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.TraceResult
import java.util.*


class AddSpanToTraceFunction {

    private var tracing = TracingConfig.tracing

    fun run(
        request: HttpRequestMessage<Optional<String>>,
        traceContextAsB3SingleFormat: String,
        providerName: String,
        context: ExecutionContext
    ): HttpResponseMessage {

        val logger = context.logger

        val traceContext = B3SingleFormat.parseB3SingleFormat(traceContextAsB3SingleFormat)

        val span = tracing!!.tracer().nextSpan(traceContext)
        span.name(providerName)
        span.start()

        try {
            span.tag("key", "secondProcessingStage")
            Thread.sleep(500)
        } finally {
            span.finish()
        }

        val result = TraceResult()
        result.status = "OK"
        result.traceContext = B3SingleFormat.writeB3SingleFormat(tracing!!.currentTraceContext().get())

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

}