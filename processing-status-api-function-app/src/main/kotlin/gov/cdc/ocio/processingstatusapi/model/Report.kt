package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import java.util.*

/**
 * Report for a given stage.
 *
 * @property uploadId String?
 * @property reportId String?
 * @property destinationId String?
 * @property eventType String?
 * @property stageName String?
 * @property contentType String?
 * @property content String?
 * @property timestamp Date
 */
data class Report(

    var id : String? = null,

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("report_id")
    var reportId: String? = null,

    @SerializedName("destination_id")
    var destinationId: String? = null,

    @SerializedName("event_type")
    var eventType: String? = null,

    @SerializedName("stage_name")
    var stageName: String? = null,

    @SerializedName("content_type")
    var contentType : String? = null,

    var content: String? = null,

    val timestamp: Date = Date()
)

/**
 * JSON serializer for Report class.
 */
class ReportSerializer : JsonSerializer<Report> {

    override fun serialize(src: Report?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {

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
