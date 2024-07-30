package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription


/**
 * Items within a issue.
 *
 * @property properties Properties?
 */
@GraphQLDescription("Contains Report issues.")
data class Items(

    @GraphQLDescription("properties of issues")
    var properties : Properties? = null
 )