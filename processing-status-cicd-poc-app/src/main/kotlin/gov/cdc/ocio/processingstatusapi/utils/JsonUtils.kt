package gov.cdc.ocio.processingstatusapi.utils

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Collection of JSON related utilities.
 */
class JsonUtils {

    companion object {

        /**
         * Removes whitespace and CRLF from json string provided.
         *
         * @param json String
         * @return String
         */
        fun minifyJson(json: String): String {
            val gson = GsonBuilder().registerTypeAdapter(String::class.java, StringSerializer()).create()
            val jsonElement = gson.fromJson(json, JsonElement::class.java)
            return gson.toJson(jsonElement)
        }
    }

    internal class StringSerializer : JsonSerializer<String?> {
        override fun serialize(src: String?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.trim { it <= ' ' })
        }
    }
}