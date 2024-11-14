package gov.cdc.ocio.processingstatusapi.models.dao

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.Instant


/**
 * Upload counts model, which is the structure returned from the uploads data projection query.
 *
 * @property reportCounts Int?
 * @property uploadId String?
 * @property latestTimestamp Instant?
 * @property jurisdiction String?
 * @property senderId String?
 * @constructor
 */
@DynamoDbBean
data class UploadCountsDao(

    var reportCounts: Int? = null,

    var uploadId: String? = null,

    var latestTimestamp: Instant? = null,

    var jurisdiction: String? = null,

    var senderId: String? = null
)
