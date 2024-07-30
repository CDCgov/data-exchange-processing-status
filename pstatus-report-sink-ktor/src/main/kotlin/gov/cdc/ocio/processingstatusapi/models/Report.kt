package gov.cdc.ocio.processingstatusapi.models


import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.models.reports.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.reports.StageInfo
import gov.cdc.ocio.processingstatusapi.models.reports.Tags
import java.util.*

/**
 * Report for a given stage.
 *
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property stageName String?
 * @property contentType String?
 * @property messageId String?
 * @property status String?
 * @property content String?
 * @property timestamp Date
 */
open class Report(

    var id : String? = null,

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("report_id")
    var reportId: String? = null,

    @SerializedName("data_stream_id")
    var dataStreamId: String? = null,

    @SerializedName("data_stream_route")
    var dataStreamRoute: String? = null,

    @SerializedName("message_metadata")
    var messageMetadata: MessageMetadata? = null,

    @SerializedName("stage_info")
    var stageInfo: StageInfo? = null,


    @SerializedName("tags")
    var tags: Tags? = null,

    @SerializedName("data")
    var data: Map<String,String>? = null,

    @SerializedName("content_type")
    var contentType : String? = null,

    @SerializedName("message_id")
    var messageId: String? = null,

    @SerializedName("jurisdiction")
    var jurisdiction: String? = null,

    @SerializedName("sender_id")
    var senderId: String? = null,

    @SerializedName("status")
    var status : String? = null,

    var content: Any? = null,

    val timestamp: Date = Date()
)
