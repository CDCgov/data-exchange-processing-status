package gov.cdc.ocio.processingstatusapi.models.dao

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.models.submission.Aggregation
import gov.cdc.ocio.processingstatusapi.models.submission.MessageMetadata


/**
 * Data access object for message metadata, which is the structure returned from CosmosDB queries.
 *
 * @property messageUUID String?
 * @property messageHash String?
 * @property aggregation Aggregation?
 * @property messageIndex Int?
 * @constructor
 */
data class MessageMetadataDao(

    @SerializedName("message_uuid")
    var messageUUID : String? = null,

    @SerializedName("message_hash")
    var messageHash: String? = null,

    var aggregation: Aggregation? = null,

    @SerializedName("message_index")
    var messageIndex: Int? = null
) {
    /**
     * Convenience function to convert a cosmos data object to a MessageMetadata object
     */
    fun toMessageMetadata(): MessageMetadata {
        return MessageMetadata().apply {
            this.messageUUID = this@MessageMetadataDao.messageUUID
            this.messageHash = this@MessageMetadataDao.messageHash
            this.aggregation = this@MessageMetadataDao.aggregation
            this.messageIndex = this@MessageMetadataDao.messageIndex
        }
    }
}