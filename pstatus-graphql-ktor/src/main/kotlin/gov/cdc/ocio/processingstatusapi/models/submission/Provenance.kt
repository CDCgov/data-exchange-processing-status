package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Provenance within a MessageMetadata.
 **
 * @property messageUUID String?
 * @property messageHash String?
 * @property singleOrBatch String?
 * @property messageIndex Int?`
 */
@GraphQLDescription("Contains Report metadata provenance.")
data class Provenance(
    @GraphQLDescription("MessageId for the metadata for that message")
    var messageId : String? = null,

    @GraphQLDescription("Unique Identifier for that message")
    var messageUUID : String? = null,

    @GraphQLDescription("MessageHash value")
    var messageHash: String? = null,

    @GraphQLDescription("Single or Batch message")
    var singleOrBatch: String? = null,

    @GraphQLDescription("Message Index")
    var messageIndex: Int? = null,

    @GraphQLDescription("Filename value")
    var fileName: String? = null,


)