package gov.cdc.ocio.processingstatusapi.functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import java.util.*

class FunctionTriggers {

    @FunctionName("HealthCheck")
    fun healthCheck(
            @HttpTrigger(name = "req", methods = [HttpMethod.GET], route = "status/health", authLevel = AuthorizationLevel.ANONYMOUS) request: HttpRequestMessage<Optional<String>>,
            context: ExecutionContext?): HttpResponseMessage {
        return HealthCheckFunction().run(request, context!!)
    }

    @FunctionName("Trace")
    fun trace(
            @HttpTrigger(name = "req", methods = [HttpMethod.POST], route = "trace/{spanName}", authLevel = AuthorizationLevel.ANONYMOUS) request: HttpRequestMessage<Optional<String>>,
            @BindingName("spanName") spanName: String?,
            context: ExecutionContext?): HttpResponseMessage {
        return TraceFunction().run(request, spanName!!, context!!)
    }

    @FunctionName("AddSpanToTrace")
    fun addSpanToTrace(
            @HttpTrigger(name = "req", methods = [HttpMethod.PUT], route = "span/{traceId}/{spanId}/{spanName}", authLevel = AuthorizationLevel.ANONYMOUS) request: HttpRequestMessage<Optional<String>>,
            @BindingName("traceId") traceId: String?,
            @BindingName("spanId") spanId: String?,
            @BindingName("spanName") spanName: String?,
            context: ExecutionContext?): HttpResponseMessage {
        return AddSpanToTraceFunction().run(request, traceId!!, spanId!!, spanName!!, context!!)
    }
}