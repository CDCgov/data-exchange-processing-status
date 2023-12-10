package gov.cdc.ocio.processingstatusapi.model.traces

import com.google.gson.annotations.SerializedName

data class TraceResult(

    @SerializedName("trace_id")
    var traceId: String? = null,

    @SerializedName("span_id")
    var spanId: String? = null,

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("destination_id")
    var destinationId: String? = null,

    @SerializedName("event_type")
    var eventType: String? = null,

    var spans: List<SpanResult>? = null
)