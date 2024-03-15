package gov.cdc.ocio.processingstatusapi;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.*;
import gov.cdc.ocio.processingstatusapi.functions.HealthCheckFunction;
import gov.cdc.ocio.processingstatusapi.functions.reports.CreateReportFunction;
import gov.cdc.ocio.processingstatusapi.functions.reports.GetReportFunction;
import gov.cdc.ocio.processingstatusapi.functions.status.GetReportCountsFunction;
import gov.cdc.ocio.processingstatusapi.functions.status.GetUploadStatusFunction;
import gov.cdc.ocio.processingstatusapi.functions.reports.ServiceBusProcessor;
import gov.cdc.ocio.processingstatusapi.functions.status.GetStatusFunction;
import gov.cdc.ocio.processingstatusapi.functions.traces.AddSpanToTraceFunction;
import gov.cdc.ocio.processingstatusapi.functions.traces.CreateTraceFunction;
import gov.cdc.ocio.processingstatusapi.functions.traces.GetSpanFunction;
import gov.cdc.ocio.processingstatusapi.functions.traces.GetTraceFunction;
import gov.cdc.ocio.processingstatusapi.model.DispositionType;

import java.util.Optional;

public class FunctionJavaWrappers {


    @FunctionName("HealthCheck")
    public HttpResponseMessage healthCheck(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "health",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request) {
        return new HealthCheckFunction(request).run();
    }

    @FunctionName("CreateTrace")
    public HttpResponseMessage createTrace(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "trace",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request) {
        return new CreateTraceFunction(request).create();
    }

    @FunctionName("TraceStartSpan")
    public HttpResponseMessage traceStartSpan(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    route = "trace/startSpan/{traceId}/{parentSpanId}",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("traceId") String traceId,
            @BindingName("parentSpanId") String parentSpanId) {
        return new AddSpanToTraceFunction(request).startSpan(traceId, parentSpanId);
    }

    @FunctionName("TraceStopSpan")
    public HttpResponseMessage traceStopSpan(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    route = "trace/stopSpan/{traceId}/{spanId}",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("traceId") String traceId,
            @BindingName("spanId") String spanId) {
        return new AddSpanToTraceFunction(request).stopSpan(traceId, spanId);
    }

    @FunctionName("GetTraceByTraceId")
    public HttpResponseMessage getTraceByTraceId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "trace/traceId/{traceId}",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("traceId") String traceId) {
        return new GetTraceFunction(request).withTraceId(traceId);
    }

    @FunctionName("GetTraceByUploadId")
    public HttpResponseMessage getTraceByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "trace/uploadId/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId) {
        return new GetTraceFunction(request).withUploadId(uploadId);
    }

    @FunctionName("GetTraceSpan")
    public HttpResponseMessage getTraceSpanByUploadIdStageName(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "trace/span",
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request) {
        return new GetSpanFunction(request).withQueryParams();
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
            @BindingName("uploadId") String uploadId) {
        return new CreateReportFunction(request, DispositionType.ADD).jsonWithUploadId(uploadId);
    }

    @FunctionName("ReplaceReportByUploadId")
    public HttpResponseMessage replaceReportByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    route = "report/json/uploadId/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId) {
        return new CreateReportFunction(request, DispositionType.REPLACE).jsonWithUploadId(uploadId);
    }

    @FunctionName("GetReportByUploadId")
    public HttpResponseMessage getReportByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "report/uploadId/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId) {
        return new GetReportFunction(request).withUploadId(uploadId);
    }

    @FunctionName("GetReportByReportId")
    public HttpResponseMessage getReportByReportId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "report/reportId/{reportId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("reportId") String reportId) {
        return new GetReportFunction(request).withReportId(reportId);
    }

    @FunctionName("GetUploadStatus")
    public HttpResponseMessage getUploadStatus(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "upload/{dataStreamId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("dataStreamId") String dataStreamId) {
        return new GetUploadStatusFunction(request).uploadStatus(dataStreamId, "dex-upload");
    }

    @FunctionName("GetReportForStage")
    public HttpResponseMessage getReportByStage(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "report/{dataStreamId}/{stageName}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("dataStreamId") String dataStreamId,
            @BindingName("stageName") String stageName,
            final ExecutionContext context) {
        context.getLogger().info("getReportByStage: dataStreamId=" + dataStreamId + ", stageName=" + stageName);
        return new GetReportFunction(request).withDataStreamId(dataStreamId, stageName);
    }

    @FunctionName("GetReportCountsByUploadId")
    public HttpResponseMessage getReportCountsByUploadId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "report/counts/{uploadId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("uploadId") String uploadId) {
        return new GetReportCountsFunction(request).withUploadId(uploadId);
    }

    @FunctionName("GetReportCountsWithQueryParams")
    public HttpResponseMessage getReportCountsWithQueryParams(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "report/counts",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request) {
        return new GetReportCountsFunction(request).withQueryParams();
    }

    @FunctionName("GetHL7InvalidStructureValidationCounts")
    public HttpResponseMessage getHL7InvalidStructureValidationCounts(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "report/counts/hl7/invalidStructureValidation",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request) {
        return new GetReportCountsFunction(request).getHL7InvalidStructureValidationCounts();
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
        return new GetStatusFunction(request).withUploadId(uploadId);
    }
}
