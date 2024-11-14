package gov.cdc.ocio.processingstatusapi.models.dao

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean

/**
 * Data access object model for retrieving an upload id from the collection.
 *
 * @property uploadId String?
 * @constructor
 */
@DynamoDbBean
data class UploadIdDao(var uploadId: String? = null)
