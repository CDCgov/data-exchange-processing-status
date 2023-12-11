package gov.cdc.ocio.processingstatusapi.model

import gov.cdc.ocio.processingstatusapi.model.reports.ReportDao
import gov.cdc.ocio.processingstatusapi.model.traces.TraceResult

data class StatusResult(

    var trace: TraceResult? = null,

    var report: ReportDao? = null
)