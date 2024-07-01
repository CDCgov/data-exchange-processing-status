package gov.cdc.ocio.processingstatusapi.model

import gov.cdc.ocio.processingstatusapi.model.reports.Report

data class StatusResult(
    var reports: List<Report>? = null
)