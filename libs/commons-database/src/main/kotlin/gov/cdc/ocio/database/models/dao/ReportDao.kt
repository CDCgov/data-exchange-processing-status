package gov.cdc.ocio.database.models.dao

import com.google.gson.Gson
import gov.cdc.ocio.database.dynamo.ReportConverterProvider
import gov.cdc.ocio.database.extensions.convertToNumber
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.protocols.jsoncore.JsonNode
import software.amazon.awssdk.protocols.jsoncore.internal.ObjectJsonNode
import java.time.Instant
import java.util.*


/**
 * Data access object for reports, which is the structure returned from CosmosDB queries.
 *
 * @property id String?
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dexIngestDateTime Date?
 * @property messageMetadata MessageMetadata?
 * @property stageInfo StageInfo?
 * @property tags Map<String,String??
 * @property data Map<String,String>?
 * @property contentType String?
 * @property timestamp Date?
 * @property content Any?
 * @property jurisdiction String?
 * @property senderId String?
 * @property dataProducerId String?
 * @property contentAsString String?
 * @constructor
 */
@DynamoDbBean(converterProviders = [
    ReportConverterProvider::class
])
open class ReportDao(

    var id : String? = null,

    var uploadId: String? = null,

    var reportId: String? = null,

    var dataStreamId: String? = null,

    var dataStreamRoute: String? = null,

    var dexIngestDateTime: Instant? = null,

    var messageMetadata: MessageMetadataDao? = null,

    var stageInfo: StageInfoDao? = null,

    var tags: Map<String, String>? = null,

    var data: Map<String, String>? = null,

    var contentType: String? = null,

    var jurisdiction: String? = null,

    var senderId: String? = null,

    var dataProducerId: String? = null,

    var timestamp: Instant? = null,

    var content: Any? = null
) {

    val contentAsString: String?
        get() {
            if (content == null) return null

            return when (contentType?.lowercase(Locale.getDefault())) {
                in setOf("application/json", "json") -> {
                    if (content is Map<*, *>) {
                        Gson().toJson(content, MutableMap::class.java).toString()
                    } else {
                        content.toString()
                    }
                }
                else -> content.toString()
            }

        }

    val contentAsMap: Map<*, *>?
        get() {
            if (content == null) return null

            return when (content) {
                is Map<*, *> -> content as Map<*, *>
                is ObjectJsonNode -> {
                    // We can't use the TableSchema.fromClass() since we can't determine all the attribute values from
                    // an Any object.
                    objectJsonNodeToMap(content as ObjectJsonNode)
                }
                else -> null
            }
        }

    /**
     * Convert the provided ObjectJsonNode to a map.
     *
     * @param node ObjectJsonNode
     * @return Map<String, Any?>
     */
    private fun objectJsonNodeToMap(node: ObjectJsonNode): Map<String, Any?> =
        objectJsonNodeToMap(node.asObject())

    /**
     * Convert the provided node map to a map.
     *
     * @param nodeMap Map<String, JsonNode>
     * @return Map<String, Any?>
     */
    private fun objectJsonNodeToMap(nodeMap: Map<String, JsonNode>): Map<String, Any?> {
        val resultMap = mutableMapOf<String, Any?>()
        nodeMap.forEach { (fieldName, fieldValue) ->
            resultMap[fieldName] = parseJsonNode(fieldValue)
        }
        return resultMap
    }

    /**
     * Parse the provided json node into its object.
     *
     * @param node JsonNode
     * @return Any?
     */
    private fun parseJsonNode(node: JsonNode): Any? {
        return when {
            node.isObject -> objectJsonNodeToMap(node.asObject())
            node.isArray -> node.asArray().map { parseJsonNode(it) }
            node.isString -> node.asString()
            node.isBoolean -> node.asBoolean()
            node.isNumber -> node.asNumber().convertToNumber()
            else -> null
        }
    }

}