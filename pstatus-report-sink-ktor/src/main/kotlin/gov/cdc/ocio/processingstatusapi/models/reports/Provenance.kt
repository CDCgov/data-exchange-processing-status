package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.annotations.SerializedName

/**
 * get Provenance.
 *
 * @property messageUUID String?
 * @property messageHash String?
 * @property singleOrBatch String?
 * @property messageIndex Int?
 */
class Provenance {

    @SerializedName("event_id")
    var messageId : String? = null

    @SerializedName("file_uuid")
    var messageUUID : String? = null

    @SerializedName("message_hash")
    var messageHash: String? = null

    @SerializedName("single_or_batch")
    var singleOrBatch: String? = null

    @SerializedName("message_index")
    var messageIndex: Int? = null




}