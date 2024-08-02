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
 * Issues within a report.
 *
 * @property level String?
 * @property message String?
 */
@GraphQLDescription("Contains Report issues.")
data class Issue(

    @GraphQLDescription("Issue level")
    var level : Level? = null,

    @GraphQLDescription("Issue message")
    var message: String? = null,
 )