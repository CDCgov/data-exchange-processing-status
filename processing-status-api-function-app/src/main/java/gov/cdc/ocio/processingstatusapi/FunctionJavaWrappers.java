package gov.cdc.ocio.processingstatusapi;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException;
import gov.cdc.ocio.processingstatusapi.functions.AddSpanToTraceFunction;
import gov.cdc.ocio.processingstatusapi.functions.HealthCheckFunction;
import gov.cdc.ocio.processingstatusapi.functions.TraceFunction;
import gov.cdc.ocio.processingstatusapi.functions.reports.AmendReportFunction;
import gov.cdc.ocio.processingstatusapi.functions.reports.CreateReportFunction;
import gov.cdc.ocio.processingstatusapi.functions.reports.GetReportFunction;
import gov.cdc.ocio.processingstatusapi.functions.reports.ServiceBusProcessor;

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
        return new CreateReportFunction(context).withHttpRequest(request);
    }

    /***
     * Process a message from the service bus queue.  The same queue is used for all
     * messages to ensure sequential processing.  For example, we need to ensure if a
     * report is created first that it can be amended.  With separate queues for creating
     * and amending there would always be a possibility that the amend message is processed
     * before the create message.
     *
     * @param message JSON message content
     * @param context Execution context of the service bus message
     */
    @FunctionName("ServiceBusProcessor")
    public void serviceBusProcessor(
            @ServiceBusQueueTrigger(
                    name = "msg",
                    queueName = "%ServiceBusQueueName%",
                    connection = "ServiceBusConnectionString"
            ) String message,
            final ExecutionContext context
    ) {
        try {
            context.getLogger().info("Received message: " + message);
            new ServiceBusProcessor(context).withMessage(message);
        } catch (Exception e) {
            context.getLogger().warning("Failed to process service bus message: " + e.getLocalizedMessage());
        }
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
