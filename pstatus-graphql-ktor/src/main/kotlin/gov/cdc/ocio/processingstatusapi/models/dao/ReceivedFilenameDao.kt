package gov.cdc.ocio.processingstatusapi.models.dao

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean

/**
 * Data access object model for retrieving a received filename from the collection.
 *
 * @property receivedFilename String?
 * @constructor
 */
@DynamoDbBean
data class ReceivedFilenameDao(
    @get:DynamoDbAttribute("received_filename")
    var receivedFilename: String? = null
)
