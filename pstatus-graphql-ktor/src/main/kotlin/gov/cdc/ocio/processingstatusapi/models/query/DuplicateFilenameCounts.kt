package gov.cdc.ocio.processingstatusapi.models.query

data class DuplicateFilenameCounts(

    var filename: String? = null,

    var totalCount: Int = 0
)