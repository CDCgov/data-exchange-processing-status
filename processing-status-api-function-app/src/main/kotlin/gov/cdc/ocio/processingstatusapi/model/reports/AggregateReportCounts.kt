package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.model.PageSummary

/**
 * Aggregate report counts
 *
 * @property summary PageSummary?
 * @property reportCountsList List<ReportCounts>?
 * @constructor
 */
data class AggregateReportCounts(

    @SerializedName("summary")
    var summary: PageSummary? = null,

    @SerializedName("uploads")
    var reportCountsList: List<ReportCounts>? = null
)