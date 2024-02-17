package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName

/**
 * Aggregate report counts
 *
 * @property summary AggregateSummary?
 * @property reportCountsList List<ReportCounts>?
 * @constructor
 */
data class AggregateReportCounts(

    @SerializedName("summary")
    var summary: AggregateSummary? = null,

    @SerializedName("uploads")
    var reportCountsList: List<ReportCounts>? = null
)