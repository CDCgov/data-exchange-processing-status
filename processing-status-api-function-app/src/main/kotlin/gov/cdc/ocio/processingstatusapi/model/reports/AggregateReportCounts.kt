package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName

data class AggregateSummary(

    @SerializedName("num_uploads")
    var numUploads: Int? = null,
)

data class AggregateReportCounts(

    @SerializedName("summary")
    var summary: AggregateSummary? = null,

    @SerializedName("uploads")
    var reportCountsList: List<ReportCounts>? = null
)