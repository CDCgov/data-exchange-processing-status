package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.model.reports.ReportV2
import gov.cdc.ocio.processingstatusapi.model.traces.TraceDao

data class StatusResultV2(

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("data_stream_id")
    var dataStreamId: String? = null,

    @SerializedName("event_type")
    var dataStreamRoute: String? = null,

    @SerializedName("trace")
    var trace: TraceDao? = null,

    var reports: List<ReportV2>? = null
)