package gov.cdc.ocio.processingstatusapi.models.dao

import gov.cdc.ocio.processingstatusapi.models.query.UndeliveredUpload
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean


/**
 * Undelivered upload data access object.
 *
 * @property uploadId String?
 * @property filename String?
 * @constructor
 */
@DynamoDbBean
data class UndeliveredUploadDao(

    var uploadId: String? = null,

    var filename: String? = null
) {
    fun toUndeliveredUpload() = UndeliveredUpload().apply {
        this.uploadId = this@UndeliveredUploadDao.uploadId
        this.filename = this@UndeliveredUploadDao.filename
    }
}