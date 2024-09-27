package gov.cdc.ocio.processingstatusapi.models.reports.inputs

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Input type for tags")
data class TagInput(
    @GraphQLDescription("Tag key")
    val key: String,

    @GraphQLDescription("Tag value")
    val value: String
)
