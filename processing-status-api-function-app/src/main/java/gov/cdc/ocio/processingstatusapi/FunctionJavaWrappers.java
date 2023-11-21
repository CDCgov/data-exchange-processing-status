package gov.cdc.ocio.processingstatusapi;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import gov.cdc.ocio.processingstatusapi.functions.*;

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
                    route = "trace",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        return new TraceFunction().run(request, context);
    }

    @FunctionName("AddSpanToTrace")
    public HttpResponseMessage addSpanToTrace(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    route = "trace/addSpan/{traceId}/{spanId}",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("traceId") String traceId,
            @BindingName("spanId") String spanId,
            final ExecutionContext context) {
        return new AddSpanToTraceFunction().run(request, traceId, spanId, context);
    }

    @FunctionName("GetTrace")
    public HttpResponseMessage getTraceById(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "trace/traceId/{traceId}",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("traceId") String traceId,
            final ExecutionContext context) {
        return new GetTraceFunction().run(request, traceId, context);
    }

    @FunctionName("GetTrace")
    public HttpResponseMessage getTraceByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "trace/uploadId/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId,
            final ExecutionContext context) {
        return new GetTraceByUploadIdFunction().run(request, uploadId, context);
    }

}
