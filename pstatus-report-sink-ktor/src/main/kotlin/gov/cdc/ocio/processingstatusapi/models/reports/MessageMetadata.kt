package gov.cdc.ocio.processingstatusapi.models.reports

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName


/**
 * Issue leve; of Report-ERROR OR WARNING
 */
enum class Aggregation {
    @SerializedName("SINGLE")
    SINGLE,
    @SerializedName("BATCH")
    BATCH
}

/**
 * Get MessageMetadata.
 *
 * @property messageUUID String?
 * @property messageHash String?
 * @property aggregation Aggregation?
 * @property messageIndex Int?
 *
 */
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