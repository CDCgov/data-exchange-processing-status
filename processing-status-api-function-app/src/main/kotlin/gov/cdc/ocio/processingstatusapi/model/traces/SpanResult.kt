package gov.cdc.ocio.processingstatusapi.model.traces

import com.google.gson.annotations.SerializedName
import java.util.*

data class SpanResult(

    @SerializedName("stage_name")
    var stageName: String? = null,

    @SerializedName("trace_id")
    var traceId: String? = null,

    @SerializedName("span_id")
    var spanId: String? = null,

    var timestamp: Date? = null,
    var status: String? = null,

    @SerializedName("elapsed_millis")
    var elapsedMillis: Long? = null,

    var metadata : List<Tags>? = null
)