package gov.cdc.ocio.types.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean


/**
 * Get MessageMetadata.
 *
 * @property messageUUID String?
 * @property messageHash String?
 * @property aggregation Aggregation?
 * @property messageIndex Int?
 *
 */
@DynamoDbBean
class MessageMetadata {

    @SerializedName("message_uuid")
    @JsonProperty("message_uuid")
    var messageUUID : String? = null

    @SerializedName("message_hash")
    @JsonProperty("message_hash")
    var messageHash: String? = null

    var aggregation: Aggregation? = null

    @SerializedName("message_index")
    @JsonProperty("message_index")
    var messageIndex: Int? = null
}