package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Properties within a issue.
 *
 * @property level String?
 * @property message Int?
 */
@GraphQLDescription("Contains issue properties.")
data class Properties(

    @GraphQLDescription("Issue level")
    var level : String? = null,

    @GraphQLDescription("Issue message")
    var message: String? = null,

)