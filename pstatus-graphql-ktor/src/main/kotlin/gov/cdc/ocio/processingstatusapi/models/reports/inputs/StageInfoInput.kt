package gov.cdc.ocio.processingstatusapi.models.reports.inputs

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.models.submission.Status
import java.time.OffsetDateTime

@GraphQLDescription("StageInfo object")
data class StageInfoInput(
    @GraphQLDescription("Name of the service associated with this report")
    val service: String? = null,

    @GraphQLDescription("Action the stage was conducting when providing this report")
    val action: String? = null,

    @GraphQLDescription("Version of the stage providing this report")
    val version: String? = null,

    @GraphQLDescription("Enumeration: [SUCCESS, FAILURE]")
    val status: Status? = null,

    @GraphQLDescription("List of issues, null if status is success")
    val issues: List<IssueInput>? = null,

    @GraphQLDescription("Timestamp of when this stage started")
    val startProcessingTime: OffsetDateTime? = null,

    @GraphQLDescription("Timestamp of when this stage finished")
    val endProcessingTime: OffsetDateTime? = null
)
