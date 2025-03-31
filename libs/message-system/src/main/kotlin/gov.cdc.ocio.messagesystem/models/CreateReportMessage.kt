package gov.cdc.ocio.messagesystem.models

import com.google.gson.annotations.SerializedName
import java.time.Instant


/**
 * Create a report service bus message.
 *
 * @property uploadId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dexIngestDateTime Date?
 * @property messageMetadata MessageMetadata?
 * @property stageInfo StageInfo?
 * @property tags String?
 * @property data Lost<KeyValue>?
 * @property jurisdiction String?
 * @property senderId String?
 * @property dataProducerId String?
 * @property contentType String?
 * @property content Any?
 */
class CreateReportMessage: MessageBase() {

    @SerializedName("report_schema_version")
    var reportSchemaVersion: String? = null

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
    var tags: Map<String,String>? = null

    @SerializedName("data")
    var data: Map<String,String>? = null

    @SerializedName("jurisdiction")
    var jurisdiction: String? = null

    @SerializedName("sender_id")
    var senderId: String? = null

    @SerializedName("data_producer_id")
    var dataProducerId: String? = null

    @SerializedName("source")
    var source: Source? = null

    @SerializedName("content_type")
    var contentType: String? = null

    // content will vary depending on content_type so make it any.  For example, if content_type is json then the
    // content type will be a Map<*, *>.
    var content: Any? = null
}