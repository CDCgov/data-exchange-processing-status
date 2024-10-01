package gov.cdc.ocio.processingstatusapi.models.reports.inputs

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.models.submission.Status
import java.time.OffsetDateTime

@GraphQLDescription("Input type for stage info")
data class StageInfoInput(
    @GraphQLDescription("Service")
    val service: String? = null,

    @GraphQLDescription("Stage name a.k.a action")
    val action: String? = null,

    @GraphQLDescription("Version")
    val version: String? = null,

    @GraphQLDescription("Status- SUCCESS OR FAILURE")
    val status: Status? = null,

    @GraphQLDescription("Issues array")
    val issues: List<IssueInput>? = null,

    @GraphQLDescription("Start processing time")
    val startProcessingTime: OffsetDateTime? = null,

    @GraphQLDescription("End processing time")
    val endProcessingTime: OffsetDateTime? = null
)
