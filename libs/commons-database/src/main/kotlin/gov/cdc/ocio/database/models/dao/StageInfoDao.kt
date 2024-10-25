package gov.cdc.ocio.database.models.dao

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
@DynamoDbBean
data class StageInfoDao(

    var service : String? = null,

    var action: String? = null,

    var version: String? = null,

    var status: Status? = null,

    var issues: List<Issue>? = null,

    var startProcessingTime: Instant? = null,

    var endProcessingTime: Instant? = null
)