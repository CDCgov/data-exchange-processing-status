package gov.cdc.ocio.processingstatusapi.functions.traces

import com.google.gson.*
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.traces.*
import gov.cdc.ocio.processingstatusapi.utils.JsonUtils
import mu.KotlinLogging
import java.util.*

/**
 * Collection of functions to get traces.
 */
class GetTraceFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {
    private val logger = KotlinLogging.logger {}

    private val gson = JsonUtils.getGsonBuilderWithUTC()

    /**
     * For a given HTTP request, this method fetches trace information for a given traceId.
     *
     * @param traceId String
     * @return HttpResponseMessage - resultant HTTP response message for the given request
     */
    fun withTraceId(traceId: String): HttpResponseMessage {

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

        logger.info("Upload Id = $uploadId")

        val traces = TraceUtils.getTraces(uploadId)
            ?: return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("The uploadId provided was not found.")
                .build()

        if (traces.size != 1) {
            return request
                .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Trace inconsistency found, expected exactly one trace for uploadId = $uploadId, but got ${traces.size}")
                .build()
        }
        val traceResult = TraceResult.buildFromTrace(traces[0])

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(gson.toJson(traceResult))
            .build()
    }

}