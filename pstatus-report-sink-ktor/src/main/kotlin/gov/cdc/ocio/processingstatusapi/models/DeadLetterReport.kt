package gov.cdc.ocio.processingstatusapi.models


import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.util.*

/**
 * Dead-LetterReport when there is missing fields or malformed data.
 *
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dispositionType DispositionType?
 * @property timestamp Date
 * @property contentType String?
 * @property content String?
 * @property deadLetterReasons List<String>
 */
data class ReportDeadLetter(

    var id : String? = null,

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("report_id")
    var reportId: String? = null,

    @SerializedName("data_stream_id")
    var dataStreamId: String? = null,

    @SerializedName("data_stream_route")
    var dataStreamRoute: String? = null,

    @SerializedName("stage_name")
    var stageName: String? = null,

    @SerializedName("disposition_type")
    var dispositionType: String? = null,


    @SerializedName("content_type")
    var contentType : String? = null,

    var content: Any? = null,

    val dexIngestDateTime: LocalDateTime = LocalDateTime.now(),

    val timestamp: Date = Date(),

    var deadLetterReasons:List<String>? = null
)
