package gov.cdc.ocio.processingstatusapi.models


import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.util.*

/**
 * Report for a given stage.
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

    @SerializedName("disposition_type")
    var dispositionType: String? = null,


    @SerializedName("content_type")
    var contentType : String? = null,

    var content: Any? = null,

    val dexIngestDateTime: LocalDateTime = LocalDateTime.now(),

    val timestamp: Date = Date(),

    var deadLetterReasons:List<String>? = null
)
