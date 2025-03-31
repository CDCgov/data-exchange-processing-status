package gov.cdc.ocio.messagesystem.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.types.model.*
import java.time.Instant


class MessageMetadataSB {

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

    fun toMessageMetadata(): MessageMetadata = MessageMetadata().apply {
        this.messageUUID = this@MessageMetadataSB.messageUUID
        this.messageHash = this@MessageMetadataSB.messageHash
        this.aggregation = this@MessageMetadataSB.aggregation
        this.messageIndex = this@MessageMetadataSB.messageIndex
    }
}

class StageInfoSB {

    var service : String? = null

    var action: String? = null

    var version: String? = null

    var status: Status? = null

    var issues: List<Issue>? = null

    @SerializedName("start_processing_time")
    @JsonProperty("start_processing_time")
    var startProcessingTime: Instant? = null

    @SerializedName("end_processing_time")
    @JsonProperty("end_processing_time")
    var endProcessingTime: Instant? = null

    fun toStageInfo(): StageInfo = StageInfo().apply {
        this.service = this@StageInfoSB.service
        this.action = this@StageInfoSB.action
        this.version = this@StageInfoSB.version
        this.status = this@StageInfoSB.status
        this.issues = this@StageInfoSB.issues
        this.startProcessingTime = this@StageInfoSB.startProcessingTime
        this.endProcessingTime = this@StageInfoSB.endProcessingTime
    }

}
