package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import java.util.*

/**
 * Report for a given stage.
 *
 * @property reportId String?
 * @property stageName String?
 * @property contentType String?
 * @property content String?
 * @property timestamp Date
 */
data class StageReport(

    @SerializedName("report_id")
    var reportId: String? = null,

    @SerializedName("stage_name")
    var stageName: String? = null,

    @SerializedName("content_type")
    var contentType : String? = null,

    var content: String? = null,

    val timestamp: Date = Date()
)

/**
 * JSON serializer for StageReport class.
 */
class StageReportSerializer : JsonSerializer<StageReport> {

    override fun serialize(src: StageReport?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {

        val jsonObject = JsonObject()

        try {
            jsonObject.add("report_id", context?.serialize(src?.reportId))
            jsonObject.add("stage_name", context?.serialize(src?.stageName))
            jsonObject.add("timestamp", context?.serialize(src?.timestamp))
            if (src?.contentType == "json") {
                jsonObject.add("content", JsonParser.parseString(src.content))
            } else {
                jsonObject.add("content", context?.serialize(src?.content))
            }
        } catch (e: Exception) {
            // do nothing
        }

        return jsonObject
    }
}
