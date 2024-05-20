package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName

data class DuplicateFilenameCounts(

    var filename: String? = null,

    @SerializedName("total_count")
    var totalCount: Long = 0
)