package gov.cdc.ocio.processingstatusapi.functions

import com.google.gson.Gson
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.Base
import gov.cdc.ocio.processingstatusapi.model.TraceResult
import io.opentelemetry.api.trace.Tracer
import org.json.JSONObject
import java.util.*

/**
 * Fetch trace information by uploadId
 *
 */
class GetTraceByUploadIdFunction {

    private var tracer: Tracer? = null

    /**
     * For a given HTTP request, this method fetches trace information for a given uploadId.
     * In order to process, the HTTP request must contain traceId.
     * @param request HttpRequestMessage<Optional<String>>
     * @param uploadId String
     * @param context ExecutionContext
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun run(
        request: HttpRequestMessage<Optional<String>>,
        uploadId: String,
        context: ExecutionContext
    ): HttpResponseMessage {
        val logger = context.logger
        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        logger.info("Upload Id = $uploadId")

        val openTelemetry = OpenTelemetryConfig.initOpenTelemetry()

        tracer = openTelemetry.getTracer(GetTraceByUploadIdFunction::class.java.name)
        val traceEndPoint = System.getenv("JAEGER_TRACE_END_POINT")+"api/traces/$uploadId"
        logger.info("traceEndPoint: $traceEndPoint")
        val response =  khttp.get(traceEndPoint)
        val obj : JSONObject = response.jsonObject
        logger.info("$obj")

        if(response.statusCode != HttpStatus.OK.value()) {
            return request
                .createResponseBuilder(HttpStatus.valueOf(response.statusCode))
                .header("Content-Type", "application/json")
                .body("Bad request. The identifier provided was not found.")
                .build()
        }

        var gson = Gson()
        var traceModel = gson.fromJson(obj.toString(), Base::class.java)

        val result = TraceResult()
        result.traceId = traceModel.data?.get(0)?.traceID
        result.spanId = traceModel.data?.get(0)?.spans?.get(0)?.spanID
        result.upload_id=""
        result.status=""
        result.elapsed=traceModel.data?.get(0)?.spans?.get(0)?.duration
        //result.timestamp = traceModel.data?.get(0)?.spans?.get(0)?.startTime
        result.destination_id=""
        result.event_type=""
        result.metadata = traceModel.data?.get(0)?.spans?.get(0)?.tags?.filter { it.key.equals("stageName") }

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

}