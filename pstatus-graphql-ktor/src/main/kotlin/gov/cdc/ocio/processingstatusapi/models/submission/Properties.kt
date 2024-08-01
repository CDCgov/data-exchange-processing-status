package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Level of Issue-ERROR OR WARNING
 */
enum class Level {

    @GraphQLDescription("Error")
    ERROR,

    @GraphQLDescription("Warning")
    WARNING
}


/**
 * Properties within a issue.
 *
 * @property level String?
 * @property message Int?
 */
@GraphQLDescription("Contains issue properties.")
data class Properties(

    @GraphQLDescription("Issue level")
    var level : Level? = null,

    @GraphQLDescription("Issue message")
    var message: String? = null,

)