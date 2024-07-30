package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * References within a report.
  * @property type String?
 * @property key  String?
 * @property value  String?
 */
@GraphQLDescription("Contains Report references.")
data class Reference(

    @GraphQLDescription("Reference type")
    var type : String? = null,

    @GraphQLDescription("Reference key")
    var key: String? = null,

    @GraphQLDescription("Reference value")
    var value: String? = null,

)