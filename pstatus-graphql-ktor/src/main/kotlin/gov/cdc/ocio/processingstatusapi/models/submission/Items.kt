package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription


/**
 * Items within a issue.
 *
 * @property properties Properties?
 */
@GraphQLDescription("Contains Report issue items.")
data class Items(

    @GraphQLDescription("Properties that define the level of the issue and the actual issue message")
    var properties : Properties? = null
 )