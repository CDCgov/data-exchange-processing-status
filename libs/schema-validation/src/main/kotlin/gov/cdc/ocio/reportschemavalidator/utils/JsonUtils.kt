package gov.cdc.ocio.reportschemavalidator.utils

import com.fasterxml.jackson.databind.JsonNode

/**
 * The utility interface for JSON operations (parsing, MIME type checks, etc.).
 */
interface JsonUtils {
    fun isJsonValid(jsonString: String): Boolean
    fun isJsonMimeType(contentType: String): Boolean
    fun parseJsonNode(message: String): JsonNode
    fun getReportSchemaVersion(message: String):String?
    fun getJsonMapOfContent(jsonString: String): Map<String, Any>
}
