package gov.cdc.ocio.processingstatusapi;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import gov.cdc.ocio.processingstatusapi.functions.AddSpanToTraceFunction;
import gov.cdc.ocio.processingstatusapi.functions.HealthCheckFunction;
import gov.cdc.ocio.processingstatusapi.functions.TraceFunction;

public class FunctionJavaWrappers {

    @FunctionName("HealthCheck")
    public HttpResponseMessage healthCheck(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "status/health",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        return new HealthCheckFunction().run(request, context);
    }

    @FunctionName("Trace")
    public HttpResponseMessage trace(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "trace/{spanName}",
                    //authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("spanName") String spanName,
            final ExecutionContext context) {
        return new TraceFunction().run(request, spanName, context);
    }

    @FunctionName("AddSpanToTrace")
    public HttpResponseMessage addSpanToTrace(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    route = "span/{traceId}/{spanId}/{spanName}",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("traceId") String traceId,
            @BindingName("spanId") String spanId,
            @BindingName("spanName") String spanName,
            final ExecutionContext context) {
        return new AddSpanToTraceFunction().run(request, traceId, spanId, spanName, context);
    }

}
