package gov.cdc.ocio.model.message

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import gov.cdc.ocio.exceptions.BadStateException
import java.lang.ClassCastException
import java.util.*

/**
 * Subscription type specifies whether the subscription is created for email,
 * websocket, SMS, etc.
 */
enum class SubscriptionType {

    @SerializedName("subscribe_email")
    EMAIL,

    @SerializedName("subscribe_websocket")
    WEBSOCKET,
}

enum class StatusType {

    @SerializedName("success")
    SUCCESS,

    @SerializedName("warning")
    WARNING,

    @SerializedName("failure")
    FAILURE
}

/**
 * Base class for all service bus messages.  Contains all the common required parameters for all service bus messages.
 * Note the ServiceBusMessage class must be *open* not *abstract* as it will need to be initially created to determine
 * the type.
 *
 * @property subscriptionType SubscriptionType
 */
open class SubscriptionSBMessage {

    @SerializedName("destination_id")
    val destinationId: String? = null

    @SerializedName("event_type")
    val eventType: String? = null

    @SerializedName("stage_name")
    val stageName: String? = null

    @SerializedName("content_type") // json or pdf or xml... if its json, we process for now
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