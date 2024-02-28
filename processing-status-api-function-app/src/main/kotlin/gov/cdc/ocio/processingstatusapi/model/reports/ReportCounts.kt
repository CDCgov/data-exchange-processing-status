package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName
import java.util.*

data class ReportCounts(

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("destination_id")
    var destinationId: String? = null,

    @SerializedName("event_type")
    var eventType: String? = null,

    var timestamp: Date? = null,

    var stages: Map<String, Any> = mapOf()
)
