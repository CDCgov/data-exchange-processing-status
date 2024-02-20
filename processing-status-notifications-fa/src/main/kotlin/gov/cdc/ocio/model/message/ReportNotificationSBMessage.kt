package gov.cdc.ocio.model.message

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import gov.cdc.ocio.exceptions.BadStateException
import java.lang.ClassCastException
import java.util.*

/**
 * Create a report service bus message.
 *
 * @property uploadId String?
 * @property destinationId String?
 * @property eventType String?
 * @property stageName String?
 * @property contentType String?
 * @property content String?
 */
class ReportNotificationSBMessage {

    @SerializedName("upload_id")
    val uploadId: String? = null

    @SerializedName("destination_id")
    val destinationId: String? = null

    @SerializedName("event_type")
    val eventType: String? = null

    @SerializedName("stage_name")
    val stageName: String? = null

    @SerializedName("content_type")
    val contentType: String? = null

    // content will vary depending on content_type so make it any.  For example, if content_type is json then the
    // content type will be a Map<*, *>.
    val content: Any? = null

    val contentAsString: String?
        get() {
            if (content == null) return null

            return when (contentType?.lowercase(Locale.getDefault())) {
                "json" -> {
                    val typeObject = object : TypeToken<HashMap<*, *>?>() {}.type
                    try {
                        Gson().toJson(content as Map<*, *>, typeObject)
                    } catch (e: ClassCastException) {
                        throw BadStateException("content_type indicates json, but the content is not in JSON format")
                    }
                }
                else -> content.toString()
            }
        }
}