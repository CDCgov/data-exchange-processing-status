package gov.cdc.ocio.processingstatusapi.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import java.time.OffsetDateTime


/**
 * DeadLetter report definition.
 *
 * @property id String?
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property stageName String?
 * @property contentType String?
 * @property messageId String?
 * @property status String?
 * @property content Map<*, *>?
 * @property timestamp OffsetDateTime?
 * @property dispositionType String?
 * @property deadLetterReasons List<String>?
 * @property validationSchemas List<String>?
 * @constructor
 */
@GraphQLDescription("Contains Report DeadLetter content.")
data class ReportDeadLetter(

    @GraphQLDescription("Identifier of the report recorded by the database")
    var id : String? = null,

    @GraphQLDescription("Upload identifier this report belongs to")
    var uploadId: String? = null,

    @GraphQLDescription("Unique report identifier")
    var reportId: String? = null,

    @GraphQLDescription("Data stream ID")
    var dataStreamId: String? = null,

    @GraphQLDescription("Data stream route")
    var dataStreamRoute: String? = null,

    @GraphQLDescription("Stage name this report is associated with")
    var stageName: String? = null,

    @GraphQLDescription("Indicates the content type of the content; e.g. JSON, XML")
    var contentType : String? = null,

    @GraphQLDescription("Message id this report belongs to; set to null if not applicable")
    var messageId: String? = null,

    @GraphQLDescription("Status this report is indicating, such as success or failure")
    var status : String? = null,

    @GraphQLDescription("Content of the report.  If the report is JSON then the content will be shown as JSON.  Otherwise, the content is a base64 encoded string.")
    var content : Map<*, *>? = null,

    @GraphQLDescription("Datestamp the report was recorded in the database")
    var timestamp: OffsetDateTime? = null,

    @GraphQLDescription("Disposition type of the report")
    var dispositionType: String? = null,

    @GraphQLDescription("List of reasons the report was sent to dead-letter")
    var deadLetterReasons: List<String>? = null,

    @GraphQLDescription("Schemas used to validate the report")
    var validationSchemas: List<String>? = null
)