package gov.cdc.ocio.processingstatusapi.functions.traces

import com.google.gson.Gson
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.traces.Base
import gov.cdc.ocio.processingstatusapi.model.traces.Data

class TraceUtils {

    companion object {

        /**
         * Locate all traces for the given uploadId.
         *
         * @param uploadId String
         * @return List<Data>?
         */
        fun getTraces(uploadId: String): List<Data>? {
            // TODO: This is very temporary!
            // This is very inefficient and won't scale at all.  Once a better understanding
            // of querying, and if it can be used to search by a tag then we'll use that.  Otherwise, we may need to
            // query the storage directly; i.e. elasticsearch, once it is available

            val traceEndPoint = System.getenv("JAEGER_TRACE_END_POINT") + "api/traces?limit=20000&service=dex-processing-status"
            val response = khttp.get(traceEndPoint)

            if (response.statusCode != HttpStatus.OK.value()) {
                return null
            }

            val obj = response.jsonObject
            val traces = Gson().fromJson(obj.toString(), Base::class.java)
            val traceMatches = traces.data.filter { data ->
                data.spans.any { span ->
                    span.tags.any { tag ->
                        tag.key.equals("uploadId") && tag.value.equals(uploadId)
                    }
                }
            }

            return traceMatches
        }
    }
}