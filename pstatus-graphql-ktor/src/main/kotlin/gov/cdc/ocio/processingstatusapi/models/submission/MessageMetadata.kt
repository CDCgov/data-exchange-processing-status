package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Aggregation of message - SINGLE or BATCH
 */
enum class Aggregation {

    @GraphQLDescription("Single")
    SINGLE,

    @GraphQLDescription("Batch")
    BATCH
}

/**
 * MessageMetadata within a report.
 *
 * @property messageUUID String?
 * @property messageHash String?
 * @property aggregation String?
 * @property messageIndex Int?`
 */
@GraphQLDescription("Report metadata containing the disposition type, message identifier, index, aggregation of whether Single or Batch and the filename")
data class MessageMetadata(

    @GraphQLDescription("Unique Identifier for that message")
    var messageUUID : String? = null,

    @GraphQLDescription("MessageHash value")
    var messageHash: String? = null,

    @GraphQLDescription("Single or Batch message")
    var aggregation: Aggregation? = null,

    @GraphQLDescription("Message Index")
    var messageIndex: Int? = null
) {
    companion object {

        /**
         * Convenience function to convert a cosmos data object to a MessageMetadata object
         */
        fun fromMessageMetadataDao(dao: gov.cdc.ocio.database.models.dao.MessageMetadataDao?) = MessageMetadata().apply {
            this.messageUUID = dao?.messageUUID
            this.messageHash = dao?.messageHash
            this.aggregation = when (dao?.aggregation) {
                gov.cdc.ocio.types.model.Aggregation.SINGLE -> Aggregation.SINGLE
                gov.cdc.ocio.types.model.Aggregation.BATCH -> Aggregation.BATCH
                else -> null
            }
            this.messageIndex = dao?.messageIndex
        }
    }
}