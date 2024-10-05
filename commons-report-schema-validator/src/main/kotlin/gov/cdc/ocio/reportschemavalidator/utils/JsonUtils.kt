package org.example.gov.cdc.ocio.reportschemavalidator.utils

import com.fasterxml.jackson.databind.JsonNode

interface JsonUtils {
    fun isJsonValid(jsonString: String): Boolean
    fun isJsonMimeType(contentType: String): Boolean
    fun parseJsonNode(message: String): JsonNode
    fun getSchemaVersion(message: String, nodeText:String):String?
}
