package gov.cdc.ocio.database.models.dao

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.database.dynamo.ReportConverterProvider
import gov.cdc.ocio.database.utils.EpochToInstantConverter
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.Instant
import java.util.*


/**
 * Data access object for dead-letter reports, which is the structure returned from CosmosDB queries.
 */
@DynamoDbBean(converterProviders = [
    ReportConverterProvider::class
])
data class ReportDeadLetterDao(
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
    var timestamp: Instant = Instant.now(),
    @SerializedName("dispositionType")
    @JsonProperty("dispositionType")
    var dispositionType: String? = null,
    @SerializedName("deadLetterReasons")
    @JsonProperty("deadLetterReasons")
    var deadLetterReasons: List<String>? = null,
    @SerializedName("validationSchemas")
    @JsonProperty("validationSchemas")
    var validationSchemas: List<String>? = null,

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
}