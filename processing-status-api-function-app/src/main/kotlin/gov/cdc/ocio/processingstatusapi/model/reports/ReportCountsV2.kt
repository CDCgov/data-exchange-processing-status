package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName

data class ReportCountsV2(

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("data_stream_id")
    var dataStreamId: String? = null,

    @SerializedName("data_stream_route")
    var dataStreamRoute: String? = null,

    var stages: Map<String, Any> = mapOf()
)
