package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.models.ServiceBusMessage


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

class MessageMetadata: ServiceBusMessage() {

    @SerializedName("message_uuid")
    var messageUUID : String? = null

    @SerializedName("message_hash")
    var messageHash: String? = null

    @SerializedName("aggregation")
    var aggregation: Aggregation? = null

    @SerializedName("message_index")
    var messageIndex: Int? = null

}