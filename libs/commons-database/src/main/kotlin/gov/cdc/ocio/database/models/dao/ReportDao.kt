package gov.cdc.ocio.database.models.dao

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.database.dynamo.ReportConverterProvider
import gov.cdc.ocio.database.utils.EpochToInstantConverter
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
    @JsonProperty("id")
    var id : String? = null,
    @JsonProperty("reportSchemaVersion")
    var reportSchemaVersion : String? = null,
    @JsonProperty("uploadId")
    var uploadId: String? = null,
    @JsonProperty("reportId")
    var reportId: String? = null,
    @JsonProperty("dataStreamId")
    var dataStreamId: String? = null,
    @JsonProperty("dataStreamRoute")
    var dataStreamRoute: String? = null,
    @SerializedName("dexIngestDateTime")
    @JsonProperty("dexIngestDateTime")
    @JsonDeserialize(using = EpochToInstantConverter::class)
    var dexIngestDateTime:  Instant? = null,
    @JsonProperty("messageMetadata")
    var messageMetadata: MessageMetadataDao? = null,
    @JsonProperty("stageInfo")
    var stageInfo: StageInfoDao? = null,
    @JsonProperty("tags")
    var tags: Map<String, String>? = null,
    @JsonProperty("data")
    var data: Map<String, String>? = null,
    @JsonProperty("content_type")
    var contentType: String? = null,
    @JsonProperty("jurisdiction")
    var jurisdiction: String? = null,
    @JsonProperty("senderId")
    var senderId: String? = null,
    @JsonProperty("dataProducerId")
    var dataProducerId: String? = null,
    @JsonProperty("source")
    var source: String? = null,
    @JsonProperty("content")
    var content: Any? = null,

    @SerializedName("timestamp")
    @JsonProperty("timestamp")
    @JsonDeserialize(using = EpochToInstantConverter::class)
    var timestamp: Instant = Instant.now()
) {
    @get:JsonIgnore
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