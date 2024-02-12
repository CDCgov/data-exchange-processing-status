package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName

data class StageCounts(

    @SerializedName("stage_name")
    var stageName: String? = null,

    var counts: Int? = null,
)
