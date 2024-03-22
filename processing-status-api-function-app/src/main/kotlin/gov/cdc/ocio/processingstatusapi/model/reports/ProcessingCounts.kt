package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName

data class ProcessingCounts(
    @SerializedName("total_counts")
    var totalCounts: Long = 0,

    @SerializedName("status_counts")
    var statusCounts: StatusCounts = StatusCounts()
)

data class StatusCounts(
    var uploading: CountsDetails = CountsDetails(),
    var failed: CountsDetails = CountsDetails(),
    var uploaded: CountsDetails = CountsDetails()
)

data class CountsDetails(
    var counts: Long = 0,
    var reasons: List<String>? = null
)
