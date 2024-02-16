package gov.cdc.ocio.processingstatusapi.utils

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import java.lang.ClassCastException
import java.util.*
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Collection of JSON related utilities.
 */
class JsonUtils {

    companion object {

        /**
         * Gson with UTC dates for serialization
         *
         * @return Gson
         */
        fun getGsonBuilderWithUTC(): Gson {
            return GsonBuilder()
                .registerTypeAdapter(Date::class.java,
                    GsonUTCDateAdapter())
                .create()
        }

    }

    internal class StringAdapter : JsonSerializer<String?> {
        override fun serialize(src: String?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.trim { it <= ' ' })
        }
    }

    internal class GsonUTCDateAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {

        override fun serialize(date: Date, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            return JsonPrimitive(format.format(date))
        }

        override fun deserialize(jsonElement: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext): Date {
            val dateString = jsonElement.asString
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                return format.parse(dateString) as Date
            } catch (e: ParseException) {
                throw JsonParseException(e)
            }
        }
    }
}