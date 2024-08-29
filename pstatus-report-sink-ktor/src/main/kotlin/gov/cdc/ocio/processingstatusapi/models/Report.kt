package gov.cdc.ocio.processingstatusapi.models

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.models.reports.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.reports.StageInfo
import java.util.*


/**
 * Report for a given stage.
 *
 * @property id String?
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dexIngestDateTime Date?
 * @property messageMetadata MessageMetadata?
 * @property stageInfo StageInfo?
 * @property tags Map<String, String>?
 * @property data Map<String, String>?
 * @property contentType String?
 * @property jurisdiction String?
 * @property senderId String?
 * @property dataProducerId String?
 * @property content Any?
 * @property timestamp Date
 * @constructor
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

    @SerializedName("dex_ingest_datetime")
    var dexIngestDateTime: Date? = null,

    @SerializedName("message_metadata")
    var messageMetadata: MessageMetadata? = null,

    @SerializedName("stage_info")
    var stageInfo: StageInfo? = null,

    @SerializedName("tags")
    var tags: Map<String, String>? = null,

    @SerializedName("data")
    var data: Map<String, String>? = null,

    @SerializedName("content_type")
    var contentType : String? = null,

    @SerializedName("jurisdiction")
    var jurisdiction: String? = null,

    @SerializedName("sender_id")
    var senderId: String? = null,

    @SerializedName("data_producer_id")
    var dataProducerId: String? = null,

    var content: Any? = null,

    val timestamp: Date = Date()
)
