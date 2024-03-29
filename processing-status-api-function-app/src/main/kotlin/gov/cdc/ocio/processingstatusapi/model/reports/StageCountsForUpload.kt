package gov.cdc.ocio.processingstatusapi.model.reports

import java.util.*

/**
 * Stage counts for a given upload.  This model is what is returned from a cosmosdb query.
 *
 * @property uploadId String?
 * @property stageName String?
 * @property counts Int?
 * @constructor
 */
data class StageCountsForUpload(

    var uploadId: String? = null,

    var schema_name: String? = null,

    var schema_version: String? = null,

    var timestamp: Date? = null,

    var stageName: String? = null,

    var counts: Int? = null,
)