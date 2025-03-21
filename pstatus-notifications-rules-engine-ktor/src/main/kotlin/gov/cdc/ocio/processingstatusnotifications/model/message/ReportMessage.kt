package gov.cdc.ocio.processingstatusnotifications.model.message

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import gov.cdc.ocio.messagesystem.models.DispositionType
import gov.cdc.ocio.processingstatusnotifications.exception.BadStateException
import java.lang.ClassCastException
import java.time.Instant
import java.util.*

enum class Status {

    @SerializedName("SUCCESS")
    SUCCESS,

    @SerializedName("FAILURE")
    FAILURE
}

data class StageInfo(
    @JsonProperty("service")
    var service : String? = null,
    @JsonProperty("action")
    var action: String? = null,
    @JsonProperty("version")
    var version: String? = null,
    @JsonProperty("status")
    var status: Status? = null,
//    @JsonProperty("issues")
//    var issues: List<Issue>? = null,

//    @SerializedName("start_processing_time")
//    @JsonProperty("start_processing_time")
//    @JsonDeserialize(using = EpochToInstantConverter::class)
//    var startProcessingTime:  Instant? = null,
//    @SerializedName("end_processing_time")
//    @JsonProperty("end_processing_time")
//    @JsonDeserialize(using = EpochToInstantConverter::class)
//    var endProcessingTime:  Instant? = null
)

/**
 * Incoming report definition.
 *
 * @property uploadId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dispositionType DispositionType?
 * @property contentType String?
 * @property stageInfo StageInfo?
 * @property content Any?
 * @property contentAsString String?
 */
class ReportMessage {

    @SerializedName("upload_id")
    val uploadId: String? = null

    @SerializedName("data_stream_id")
    val dataStreamId: String? = null

    @SerializedName("data_stream_route")
    val dataStreamRoute: String? = null

    @SerializedName("disposition_type")
    val dispositionType: DispositionType? = null

    @SerializedName("content_type")
    val contentType: String? = null

    @SerializedName("stage_info")
    val stageInfo: StageInfo? = null

    // content will vary depending on content_type so make it any.  For example, if content_type is json then the
    // content type will be a Map<*, *>.
    val content: Any? = null

//    val contentAsString: String?
//        get() {
//            if (content == null) return null
//
//            return when (contentType?.lowercase(Locale.getDefault())) {
//                "json", "application/json" -> {
//                    val typeObject = object : TypeToken<HashMap<*, *>?>() {}.type
//                    try {
//                        Gson().toJson(content as Map<*, *>, typeObject)
//                    } catch (e: ClassCastException) {
//                        throw BadStateException("content_type indicates json, but the content is not in JSON format")
//                    }
//                }
//                else -> content.toString()
//            }
//        }
}