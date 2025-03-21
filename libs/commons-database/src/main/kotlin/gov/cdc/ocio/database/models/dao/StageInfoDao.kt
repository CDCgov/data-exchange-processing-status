package gov.cdc.ocio.database.models.dao

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.database.dynamo.ReportConverterProvider
import gov.cdc.ocio.database.models.Issue
import gov.cdc.ocio.database.models.Status
import gov.cdc.ocio.types.adapters.EpochToInstantConverter
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.Instant
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize


/**
 * Data access object for report stage info, which is the structure returned from CosmosDB queries.
 *
 * @property service String?
 * @property action String?
 * @property version String?
 * @property status Status?
 * @property issues List<Issue>?
 * @property startProcessingTime Date?
 * @property endProcessingTime Date?
 * @constructor
 */
@DynamoDbBean(converterProviders = [
    ReportConverterProvider::class
])
data class StageInfoDao(
    @JsonProperty("service")
    var service : String? = null,
    @JsonProperty("action")
    var action: String? = null,
    @JsonProperty("version")
    var version: String? = null,
    @JsonProperty("status")
    var status: Status? = null,
    @JsonProperty("issues")
    var issues: List<Issue>? = null,

    @SerializedName("start_processing_time")
    @JsonProperty("start_processing_time")
    @JsonDeserialize(using = EpochToInstantConverter::class)
    var startProcessingTime:  Instant? = null,
    @SerializedName("end_processing_time")
    @JsonProperty("end_processing_time")
    @JsonDeserialize(using = EpochToInstantConverter::class)
    var endProcessingTime:  Instant? = null
)