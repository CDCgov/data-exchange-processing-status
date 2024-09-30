package gov.cdc.ocio.database.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.Instant


/**
 * Get StageInfo from the report message.
 *
 * @property service String?
 * @property action String?
 * @property version String?
 * @property status Status?
 * @property issues List<Issue>?
 * @property startProcessingTime Instant?
 * @property endProcessingTime Instant?
 */
@DynamoDbBean
class StageInfo {

    var service : String? = null

    var action: String? = null

    var version: String? = null

    var status: Status? = null

    var issues: List<Issue>? = null

    @SerializedName("start_processing_time")
    @JsonProperty("start_processing_time")
    var startProcessingTime: Instant? = null

    @SerializedName("end_processing_time")
    @JsonProperty("end_processing_time")
    var endProcessingTime: Instant? = null
}