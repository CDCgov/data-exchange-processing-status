package gov.cdc.ocio.processingstatusapi.model.reports

import java.util.*

data class UploadCounts(

    var reportCounts: Int? = null,

    var uploadId: String? = null,

    var latestTimestamp: Date? = null
)
