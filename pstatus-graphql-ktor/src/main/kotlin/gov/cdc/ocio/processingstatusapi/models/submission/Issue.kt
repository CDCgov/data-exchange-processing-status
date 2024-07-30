package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Items within a issue.
 *
 * @property items Items?
 */
@GraphQLDescription("Contains Report issues.")
data class Issue(

    @GraphQLDescription("Issues")
    var items : Items? = null
 )