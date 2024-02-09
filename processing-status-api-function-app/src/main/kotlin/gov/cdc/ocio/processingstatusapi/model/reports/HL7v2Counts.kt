package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName

class HL7v2Counts {

    @SerializedName("upload_id")
    var uploadId: String? = null

    @SerializedName("destination_id")
    var destinationId: String? = null

    @SerializedName("event_type")
    var eventType: String? = null

    @SerializedName("report_count")
    var reportCounts: MutableList<ReportCounts>? = null
}
