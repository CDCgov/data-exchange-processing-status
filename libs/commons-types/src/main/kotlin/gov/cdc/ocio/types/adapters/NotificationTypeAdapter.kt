package gov.cdc.ocio.types.adapters

import com.google.gson.*
import java.lang.reflect.Type
import gov.cdc.ocio.types.model.*


class NotificationTypeAdapter : JsonDeserializer<Notification> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Notification {
        val jsonObject = json.asJsonObject
        return when (val type = jsonObject["notificationType"].asString) {
            "EMAIL" -> context.deserialize(jsonObject, EmailNotification::class.java)
            "WEBHOOK" -> context.deserialize(jsonObject, WebhookNotification::class.java)
            else -> throw JsonParseException("Unknown notification type: $type")
        }
    }
}

