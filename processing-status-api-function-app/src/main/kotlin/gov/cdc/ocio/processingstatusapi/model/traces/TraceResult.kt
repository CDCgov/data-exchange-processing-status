package gov.cdc.ocio.processingstatusapi.model.traces

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.model.traces.Tags
import java.util.*

data class TraceResult(

    @SerializedName("trace_id")
    var traceId: String? = null,

    @SerializedName("span_id")
    var spanId: String? = null,

    @SerializedName("upload_id")
    var uploadId: String? = null,

    var timestamp: Date? = null,
    var status: String? = null,

    @SerializedName("elapsed_millis")
    var elapsedMillis: Long? = null,

    @SerializedName("destination_id")
    var destinationId: String? = null,

    @SerializedName("event_type")
    var eventType: String? = null,

    var metadata : List<Tags>? = null
)