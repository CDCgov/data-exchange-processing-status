package gov.cdc.ocio.processingstatusapi;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import gov.cdc.ocio.processingstatusapi.functions.AddSpanToTraceFunction;
import gov.cdc.ocio.processingstatusapi.functions.HealthCheckFunction;
import gov.cdc.ocio.processingstatusapi.functions.TraceFunction;
import gov.cdc.ocio.processingstatusapi.functions.reports.AmendReportFunction;
import gov.cdc.ocio.processingstatusapi.functions.reports.CreateReportFunction;
import gov.cdc.ocio.processingstatusapi.functions.reports.GetReportFunction;

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

    @FunctionName("CreateReport")
    public HttpResponseMessage createReport(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "report",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        return new CreateReportFunction(request, context).run();
    }

    @FunctionName("AmendReportByUploadId")
    public HttpResponseMessage amendReportByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    route = "report/uploadId/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId,
            final ExecutionContext context) {
        return new AmendReportFunction(request, context).withUploadId(uploadId);
    }

    @FunctionName("AmendReportByReportId")
    public HttpResponseMessage amendReportByReportId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    route = "report/reportId/{reportId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("reportId") String reportId,
            final ExecutionContext context) {
        return new AmendReportFunction(request, context).withReportId(reportId);
    }

    @FunctionName("GetReportByUploadId")
    public HttpResponseMessage getReportByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "report/uploadId/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId,
            final ExecutionContext context) {
        return new GetReportFunction(request, context).withUploadId(uploadId);
    }

    @FunctionName("GetReportByReportId")
    public HttpResponseMessage getReportByReportId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "report/reportId/{reportId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("reportId") String reportId,
            final ExecutionContext context) {
        return new GetReportFunction(request, context).withReportId(reportId);
    }
}
