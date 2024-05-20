package gov.cdc.ocio.processingstatusapi.model.reports

data class DuplicateFilenameCounts(

    var filename: String? = null,

    var totalCount: Long = 0
)