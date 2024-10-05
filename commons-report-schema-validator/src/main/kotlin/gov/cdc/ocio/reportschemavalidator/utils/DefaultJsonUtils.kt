package gov.cdc.ocio.gov.cdc.ocio.reportschemavalidator.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.example.gov.cdc.ocio.reportschemavalidator.utils.JsonUtils
import java.awt.datatransfer.MimeTypeParseException
import javax.activation.MimeType

class DefaultJsonUtils:JsonUtils {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    override fun isJsonValid(jsonString: String): Boolean {
        return try {
           objectMapper.readTree(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun isJsonMimeType(contentType: String): Boolean {
        return try {
            val mimeType = MimeType(contentType)
            mimeType.primaryType == "json" || (mimeType.primaryType == "application" && mimeType.subType == "json")
        } catch (e: MimeTypeParseException) {
            false
        }
    }

    override fun parseJsonNode(message: String): JsonNode {
          return objectMapper.readTree(message)
    }

    override fun getSchemaVersion(message: String, nodeText:String): String? {
        val jsonNode = objectMapper.readTree(message)
        return jsonNode.get(nodeText)?.asText().takeIf { !it.isNullOrEmpty() } //"report_schema_version"
    }
}