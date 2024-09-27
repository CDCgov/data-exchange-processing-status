package gov.cdc.ocio.processingstatusapi.models.reports.inputs

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Input type for issues")
data class IssueInput(
    @GraphQLDescription("Issue code")
    val code: String? = null,

    @GraphQLDescription("Issue description")
    val description: String? = null
)
