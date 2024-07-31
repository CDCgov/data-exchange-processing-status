package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Issues within a report.
 *
 * @property items Items?
 */
@GraphQLDescription("Contains Report issues.")
data class Issue(

    @GraphQLDescription("Items")
    var items : Items? = null
 )