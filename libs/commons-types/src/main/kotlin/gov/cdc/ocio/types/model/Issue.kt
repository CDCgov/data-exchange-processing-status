package gov.cdc.ocio.types.model

import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean


/**
 * Get issues array in report message.
 *
 * @property level String?
 * @property message String?
 */
@DynamoDbBean
class Issue {

    @SerializedName("level")
    var level : Level? = null

    @SerializedName("message")
    var message: String? = null
}