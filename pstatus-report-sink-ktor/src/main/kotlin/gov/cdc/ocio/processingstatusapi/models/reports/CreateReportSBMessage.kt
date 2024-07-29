package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.models.ServiceBusMessage


/**
 * Create a report service bus message.
 *
 * @property uploadId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dataStreamRoute String?
 * @property messageMetadata MessageMetadata?
 * @property StageInfo StageInfo?
 * @property tags String?
 * @property data Lost<KeyValue>?
 * @property contentType String?
 * @property messageId String?
 * @property content Any?
 */
class CreateReportSBMessage: ServiceBusMessage() {

    @SerializedName("upload_id")
    val uploadId: String? = null

    @SerializedName("data_stream_id")
    val dataStreamId: String? = null

    @SerializedName("data_stream_route")
    val dataStreamRoute: String? = null

    @SerializedName("message_metadata")
    val messageMetadata: MessageMetadata? = null

    @SerializedName("stage_info")
    val stageInfo: StageInfo? = null

    @SerializedName("tags")
    val tags: Tags? = null

    @SerializedName("data")
    val data: Map<String,String>? = null

    @SerializedName("content_type")
    val contentType: String? = null

    @SerializedName("message_id")
    var messageId: String? = null

/*    @SerializedName("status")
    var status : String? = null*/

    // content will vary depending on content_type so make it any.  For example, if content_type is json then the
    // content type will be a Map<*, *>.
    var content: Any? = null


}