package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * References within a report.
 * @property name  String?
 */
@GraphQLDescription("Contains Report references.")
data class Tags(

    @GraphQLDescription("Tag name")
    var name : String? = null,
)