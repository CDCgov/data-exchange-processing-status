package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Issue within a report.
 *
 * @property level String?
 * @property message Int?
 */
@GraphQLDescription("Contains Report issues.")
data class Properties(

    @GraphQLDescription("Issue level")
    var level : String? = null,

    @GraphQLDescription("Issue message")
    var message: String? = null,

)