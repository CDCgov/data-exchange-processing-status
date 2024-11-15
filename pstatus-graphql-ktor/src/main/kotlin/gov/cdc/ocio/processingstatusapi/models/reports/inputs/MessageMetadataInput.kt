package gov.cdc.ocio.processingstatusapi.models.reports.inputs

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.models.submission.Aggregation

@GraphQLDescription("Message metadata object")
data class MessageMetadataInput(
    @GraphQLDescription("Unique ID of the message")
    val messageUUID: String? = null,

    @GraphQLDescription("MD5 hash of the message content")
    val messageHash: String? = null,

    @GraphQLDescription("Enumeration: [SINGLE, BATCH]")
    val aggregation: Aggregation? = null,

    @GraphQLDescription("Index of the message; e.g. row if csv")
    val messageIndex: Int? = null
)