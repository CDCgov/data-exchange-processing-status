package gov.cdc.ocio.processingstatusapi.models.reports

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.models.ServiceBusMessage
import gov.cdc.ocio.database.models.Issue
import gov.cdc.ocio.database.models.StageInfo
import gov.cdc.ocio.database.models.Status
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

/**
 * Create a report service bus message.
 *
 * @property uploadId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dexIngestDateTime Instant?
 * @property messageMetadata MessageMetadataSB?
 * @property stageInfo StageInfoSB?
 * @property tags Map<String, String>?
 * @property data Map<String, String>?
 * @property jurisdiction String?
 * @property senderId String?
 * @property contentType String?
 * @property content Any?
 */
class CreateReportSBMessage: ServiceBusMessage() {

    @SerializedName("upload_id")
    var uploadId: String? = null

    @SerializedName("data_stream_id")
    var dataStreamId: String? = null

    @SerializedName("data_stream_route")
    var dataStreamRoute: String? = null

    @SerializedName("dex_ingest_datetime")
    var dexIngestDateTime: Instant? = null

    @SerializedName("message_metadata")
    var messageMetadata: MessageMetadataSB? = null

    @SerializedName("stage_info")
    var stageInfo: StageInfoSB? = null

    @SerializedName("tags")
    var tags: Map<String, String>? = null

    @SerializedName("data")
    var data: Map<String, String>? = null

    @SerializedName("jurisdiction")
    var jurisdiction: String? = null

    @SerializedName("sender_id")
    var senderId: String? = null

    @SerializedName("content_type")
    var contentType: String? = null

    // content will vary depending on content_type so make it any.  For example, if content_type is json then the
    // content type will be a Map<*, *>.
    var content: Any? = null
}