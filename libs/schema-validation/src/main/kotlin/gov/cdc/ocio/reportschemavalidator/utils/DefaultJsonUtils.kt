package gov.cdc.ocio.reportschemavalidator.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.awt.datatransfer.MimeTypeParseException
import javax.activation.MimeType

/**
 * The class which defines utility methods for JSON operations (e.g., MIME type validation).
 */
class DefaultJsonUtils(private val objectMapper: ObjectMapper ):JsonUtils {
    /**
     * The function which checks whether the json string is valid json or not
     * @param jsonString String
     * @return Boolean
     */
    override fun isJsonValid(jsonString: String): Boolean {
        return try {
            objectMapper.readTree(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * The function which validates the MIME type
     * @param contentType String
     * @return Boolean
     */
    override fun isJsonMimeType(contentType: String): Boolean {
        return try {
            val mimeType = MimeType(contentType)
            mimeType.primaryType == "json" || (mimeType.primaryType == "application" && mimeType.subType == "json")
        } catch (e: MimeTypeParseException) {
            false
        }
    }
    /**
     * The function which parses the jsonNode
     * @param message String
     * @return JsonNode
     */
    override fun parseJsonNode(message: String): JsonNode {
        return objectMapper.readTree(message)
    }
    /**
     * The function which gets the schema version from the message and nodeText
     * @param message String
     * @return String?
     */

    override fun getReportSchemaVersion(message: String): String? {
        val jsonNode = objectMapper.readTree(message)
        return jsonNode.get("report_schema_version")?.asText().takeIf { !it.isNullOrEmpty() } //"report_schema_version"
    }

    /**
     * Converts the JSON string provided to a map.
     *
     * @param jsonString String
     * @return Map<String, Any>
     */
    override fun getJsonMapOfContent(jsonString: String): Map<String, Any> {
        val response = runCatching {
            val typeObject = object : TypeToken<Map<String, Any>>() {}.type
            val result: Map<String, Any> = Gson().fromJson(jsonString, typeObject)
            return@runCatching result
        }

        return when (response.isSuccess) {
            true -> response.getOrDefault(mapOf())
            else -> mapOf("failure" to (response.exceptionOrNull()?.localizedMessage ?: "error"))
        }
    }
}