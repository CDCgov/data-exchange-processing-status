package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * MessageMetadata within a report.
 * @property provenance  Provenance

 */
@GraphQLDescription("MessageMetadata")
data class MessageMetadata(

    @GraphQLDescription("DispositionType")
    var dispositionType: String?= null,

   @GraphQLDescription("Provenance")
    var provenance: Provenance? = null


)