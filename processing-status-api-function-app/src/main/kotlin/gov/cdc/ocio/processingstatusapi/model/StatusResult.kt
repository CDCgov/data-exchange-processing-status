package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import gov.cdc.ocio.processingstatusapi.model.traces.TraceDao

data class StatusResult(

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("destination_id")
    var destinationId: String? = null,

    @SerializedName("event_type")
    var eventType: String? = null,

    @SerializedName("trace")
    var trace: TraceDao? = null,

    var reports: List<Report>? = null
)