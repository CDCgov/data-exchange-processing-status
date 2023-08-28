package gov.cdc.ocio.processingstatusapi;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
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
                    methods = {HttpMethod.GET},
                    route = "trace/{providerName}",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BindingName("providerName") String providerName,
            final ExecutionContext context) {
        return new TraceFunction().run(request, providerName, context);
    }

}
