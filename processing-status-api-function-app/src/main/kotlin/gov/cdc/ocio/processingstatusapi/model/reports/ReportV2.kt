package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
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
 * @property content String?
 * @property timestamp Date
 */
data class ReportV2 (

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

    @SerializedName("content_type")
    var contentType : String? = null,

    var content: Any? = null,

    val timestamp: Date = Date()
) {
    val contentAsString: String?
        get() {
            if (content == null) return null

            return when (contentType?.lowercase(Locale.getDefault())) {
                "json" -> {
                    if (content is LinkedHashMap<*, *>)
                        Gson().toJson(content, MutableMap::class.java).toString()
                    else
                        content.toString()
                }
                else -> content.toString()
            }
        }
}

/**
 * JSON serializer for ReportV2 class.
 */
class ReportSerializerV2 : JsonSerializer<ReportV2> {

    override fun serialize(src: ReportV2?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {

        val jsonObject = JsonObject()

        try {
            jsonObject.add("report_id", context?.serialize(src?.reportId))
            jsonObject.add("stage_name", context?.serialize(src?.stageName))
            jsonObject.add("timestamp", context?.serialize(src?.timestamp))
            jsonObject.add("content", context?.serialize(src?.content))
        } catch (e: Exception) {
            // do nothing
        }

        return jsonObject
    }
}