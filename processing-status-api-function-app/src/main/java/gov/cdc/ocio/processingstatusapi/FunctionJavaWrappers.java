package gov.cdc.ocio.processingstatusapi;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import gov.cdc.ocio.processingstatusapi.functions.*;
import gov.cdc.ocio.processingstatusapi.functions.reports.*;
import gov.cdc.ocio.processingstatusapi.functions.status.GetStatusFunction;
import gov.cdc.ocio.processingstatusapi.functions.traces.AddSpanToTraceFunction;
import gov.cdc.ocio.processingstatusapi.functions.traces.GetTraceFunction;
import gov.cdc.ocio.processingstatusapi.functions.traces.CreateTraceFunction;
import gov.cdc.ocio.processingstatusapi.model.DispositionType;

public class FunctionJavaWrappers {

    @FunctionName("HealthCheck")
    public HttpResponseMessage healthCheck(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "health",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        return new HealthCheckFunction(request, context).run();
    }

    @FunctionName("CreateTrace")
    public HttpResponseMessage createTrace(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "trace",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        return new CreateTraceFunction(request, context).create();
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
        return new AddSpanToTraceFunction(request, context).addSpan(traceId, spanId);
    }

    @FunctionName("GetTraceByTraceId")
    public HttpResponseMessage getTraceByTraceId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "trace/traceId/{traceId}",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("traceId") String traceId,
            final ExecutionContext context) {
        return new GetTraceFunction(request, context).withTraceId(traceId);
    }

    @FunctionName("GetTraceByUploadId")
    public HttpResponseMessage getTraceByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "trace/uploadId/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId,
            final ExecutionContext context) {
        return new GetTraceFunction(request, context).withUploadId(uploadId);
    }

    /***
     * Process a message from the service bus queue.
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

    @FunctionName("CreateReportByUploadId")
    public HttpResponseMessage createReportByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "report/json/uploadId/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId,
            final ExecutionContext context) {
        return new CreateReportFunction(request, context, DispositionType.ADD).jsonWithUploadId(uploadId);
    }

    @FunctionName("ReplaceReportByUploadId")
    public HttpResponseMessage replaceReportByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    route = "report/json/uploadId/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId,
            final ExecutionContext context) {
        return new CreateReportFunction(request, context, DispositionType.REPLACE).jsonWithUploadId(uploadId);
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

    @FunctionName("GetUploadStatus")
    public HttpResponseMessage getUploadStatus(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "upload/{destinationId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("destinationId") String destinationId,
            final ExecutionContext context) {
        return new GetUploadStatusFunction(request, context).uploadStatus(destinationId, "dex-upload");
    }

    @FunctionName("GetReportForStage")
    public HttpResponseMessage getReportByStage(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "report/{destinationId}/{stageName}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("destinationId") String destinationId,
            @BindingName("stageName") String stageName,
            final ExecutionContext context) {
        context.getLogger().info("getReportByStage: destinationId=" + destinationId + ", stageName=" + stageName);
        return new GetReportFunction(request, context).withDestinationId(destinationId, stageName);
    }

    @FunctionName("GetStatusByUploadId")
    public HttpResponseMessage getStatusByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "status/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId,
            final ExecutionContext context) {
        context.getLogger().info("getStatusByUploadId: uploadId=" + uploadId);
        return new GetStatusFunction(request, context).withUploadId(uploadId);
    }
}
