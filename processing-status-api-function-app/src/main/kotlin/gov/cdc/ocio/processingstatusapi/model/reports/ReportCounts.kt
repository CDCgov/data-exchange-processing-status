package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName
import java.util.*

data class ReportCounts(

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("data_stream_id")
    var dataStreamId: String? = null,

    @SerializedName("data_stream_route")
    var dataStreamRoute: String? = null,

    var timestamp: Date? = null,

    var stages: Map<String, Any> = mapOf()
)
