package gov.cdc.ocio.processingstatusapi.functions.traces

import com.google.gson.*
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.traces.*
import gov.cdc.ocio.processingstatusapi.utils.JsonUtils
import java.util.*

/**
 * Collection of functions to get traces.
 */
class GetTraceFunction(
    private val request: HttpRequestMessage<Optional<String>>,
    context: ExecutionContext
) {
    private val logger = context.logger

    private val gson = JsonUtils.getGsonBuilderWithUTC()

    /**
     * For a given HTTP request, this method fetches trace information for a given traceId.
     *
     * @param traceId String
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun withTraceId(traceId: String): HttpResponseMessage {

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        logger.info("Trace Id = $traceId")

        val traceEndPoint = System.getenv("JAEGER_TRACE_END_POINT")+"api/traces/$traceId"
        logger.info("traceEndPoint: $traceEndPoint")
        val response =  khttp.get(traceEndPoint)
        val obj = response.jsonObject
        logger.info("$obj")

        if (response.statusCode != HttpStatus.OK.value()) {
            return request
                .createResponseBuilder(HttpStatus.valueOf(response.statusCode))
                .body("Bad request. The identifier provided was not found.")
                .build()
        }

        val traceModel = Gson().fromJson(obj.toString(), Base::class.java)
        val result = TraceResult.buildFromTrace(traceModel.data[0])

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(gson.toJson(result))
            .build()
    }

    /**
     * For a given HTTP request, this method fetches trace information for a given uploadId.
     *
     * @param uploadId String
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun withUploadId(uploadId: String): HttpResponseMessage {

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        logger.info("Upload Id = $uploadId")

        val traceEndPoint = System.getenv("JAEGER_TRACE_END_POINT")+"api/traces/$uploadId"
        logger.info("traceEndPoint: $traceEndPoint")
        val response = khttp.get(traceEndPoint)
        val obj = response.jsonObject
        logger.info("$obj")

        if (response.statusCode != HttpStatus.OK.value()) {
            return request
                .createResponseBuilder(HttpStatus.valueOf(response.statusCode))
                .header("Content-Type", "application/json")
                .body("Bad request. The identifier provided was not found.")
                .build()
        }

        val traceModel = Gson().fromJson(obj.toString(), Base::class.java)
        val result = TraceResult.buildFromTrace(traceModel.data[0])

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(gson.toJson(result))
            .build()
    }

    companion object {
        val excludedSpanTags = listOf("spanMark", "span.kind", "internal.span.format")
    }
}