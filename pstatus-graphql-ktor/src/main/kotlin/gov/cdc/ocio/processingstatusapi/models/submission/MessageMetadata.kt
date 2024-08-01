package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Status of Report-SUCCESS OR FAILURE
 */
enum class Aggregation {

    @GraphQLDescription("Single")
    SINGLE,

    @GraphQLDescription("Batch")
    FAILURE
}


/**
 * MessageMetadata within a report.
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
    var messageIndex: Int? = null,

)