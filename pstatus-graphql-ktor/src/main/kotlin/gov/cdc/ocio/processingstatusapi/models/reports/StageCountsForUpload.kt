package gov.cdc.ocio.processingstatusapi.models.reports

import java.time.OffsetDateTime

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

    var timestamp: OffsetDateTime? = null,

    var stageName: String? = null,

    var counts: Int? = null,
)