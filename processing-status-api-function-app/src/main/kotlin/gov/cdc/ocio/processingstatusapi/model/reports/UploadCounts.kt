package gov.cdc.ocio.processingstatusapi.model.reports

import java.util.*

/**
 * DEX Upload Counts model, which is the structure returned from the uploads data projection cosmosdb query.
 *
 * @property reportCounts Int?
 * @property uploadId String?
 * @property latestTimestamp Date?
 * @constructor
 */
data class UploadCounts(

    var reportCounts: Int? = null,

    var uploadId: String? = null,

    var latestTimestamp: Date? = null
)
