package gov.cdc.ocio.processingstatusapi.models.query

import java.time.Instant


/**
 * DEX Upload Counts model, which is the structure returned from the uploads data projection cosmosdb query.
 * @property reportCounts Int?
 * @property uploadId String?
 * @property latestTimestamp Instant?
 * @property jurisdiction String?
 * @property senderId String?
 * @constructor
 */
data class UploadCounts(

    var reportCounts: Int? = null,

    var uploadId: String? = null,

    var latestTimestamp: Instant? = null,

    var jurisdiction: String? = null,

    var senderId: String? = null
)
