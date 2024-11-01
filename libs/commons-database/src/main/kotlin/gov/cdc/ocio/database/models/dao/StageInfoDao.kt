package gov.cdc.ocio.database.models.dao

import com.fasterxml.jackson.annotation.JsonProperty
import gov.cdc.ocio.database.dynamo.ReportConverterProvider
import gov.cdc.ocio.database.models.Issue
import gov.cdc.ocio.database.models.Status
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.Instant


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
    @JsonProperty("start_processing_time")
    var startProcessingTime: Instant? = null,
    @JsonProperty("end_processing_time")
    var endProcessingTime: Instant? = null
)