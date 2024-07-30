package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import java.time.OffsetDateTime

/**
 * Message stageInfo within a Report.
 **
 * @property service String?
 * @property stage String?
 * @property version String?
 * @property status String?
 * @property startProcessingTime Date?
 * @property endProcessingTime Date?
 */
@GraphQLDescription("StageInfo.")
data class StageInfo(

    @GraphQLDescription("Service")
    var service : String? = null,

    @GraphQLDescription("Stage name a.k.a action")
    var stage: String? = null,

    @GraphQLDescription("Version")
    var version: String? = null,

    @GraphQLDescription("Status- SUCCESS OR FAILURE")
    var status: String? = null,

    @GraphQLDescription("Issues array")
    var issues: List<Issue>? = null,

    @GraphQLDescription("Issues array")
    var startProcessingTime: OffsetDateTime? = null,

    @GraphQLDescription("Issues array")
    var endProcessingTime: OffsetDateTime? = null

)