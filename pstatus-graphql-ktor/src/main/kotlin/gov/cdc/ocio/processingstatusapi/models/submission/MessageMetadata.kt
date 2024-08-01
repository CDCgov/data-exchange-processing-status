package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * MessageMetadata within a report.
 * @property provenance  Provenance

 */
@GraphQLDescription("Report metadata containing the disposition type, message identifier, index, aggregation of whether Single or Batch and the filename")
data class MessageMetadata(

    @GraphQLDescription("DispositionType")
    var dispositionType: String?= null,

   @GraphQLDescription("Provenance")
    var provenance: Provenance? = null



)