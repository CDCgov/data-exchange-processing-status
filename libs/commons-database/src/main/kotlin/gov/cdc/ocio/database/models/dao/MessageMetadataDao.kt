package gov.cdc.ocio.database.models.dao

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.types.model.Aggregation
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean


/**
 * Data access object for message metadata, which is the structure returned from CosmosDB queries.
 *
 * @property messageUUID String?
 * @property messageHash String?
 * @property aggregation Aggregation?
 * @property messageIndex Int?
 * @constructor
 */
@DynamoDbBean
data class MessageMetadataDao(

    @SerializedName("message_uuid")
    var messageUUID : String? = null,

    @SerializedName("message_hash")
    var messageHash: String? = null,

    var aggregation: Aggregation? = null,

    @SerializedName("message_index")
    var messageIndex: Int? = null
)