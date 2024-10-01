package gov.cdc.ocio.processingstatusapi.models.reports.inputs

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.models.submission.Aggregation

@GraphQLDescription("Input type for message metadata")
data class MessageMetadataInput(
    @GraphQLDescription("Unique Identifier for that message")
    val messageUUID: String? = null,

    @GraphQLDescription("MessageHash value")
    val messageHash: String? = null,

    @GraphQLDescription("Single or Batch message")
    val aggregation: Aggregation? = null,

    @GraphQLDescription("Message Index")
    val messageIndex: Int? = null
)