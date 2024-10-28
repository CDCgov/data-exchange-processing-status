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
    var message: String? = null
) {
    companion object {

        /**
         * Convenience function to convert a database object to a StageInfo object
         */
        fun fromIssueDao(dao: gov.cdc.ocio.database.models.Issue?) = Issue().apply {
            this.level = when (dao?.level) {
                gov.cdc.ocio.database.models.Level.ERROR -> Level.ERROR
                gov.cdc.ocio.database.models.Level.WARNING -> Level.WARNING
                else -> null
            }
            this.message = dao?.message
        }
    }
}