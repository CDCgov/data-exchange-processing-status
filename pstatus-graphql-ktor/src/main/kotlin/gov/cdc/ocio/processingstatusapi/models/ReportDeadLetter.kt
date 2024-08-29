package gov.cdc.ocio.processingstatusapi.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.models.submission.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.submission.StageInfo
import java.time.OffsetDateTime


/**
 * DeadLetter report definition.
 *
 * @property id String?
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dexIngestDateTime OffsetDateTime?
 * @property messageMetadata MessageMetadata?
 * @property stageInfo StageInfo?
 * @property tags Map<String, String>?
 * @property data Map<String, String>?
 * @property contentType String?
 * @property jurisdiction String?
 * @property senderId String?
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

    @GraphQLDescription("Date/time of when the upload was first ingested into the data-exchange")
    var dexIngestDateTime: OffsetDateTime? = null,

    @GraphQLDescription("Metadata for the message (if applicable) associated with this report")
    var messageMetadata: MessageMetadata? = null,

    @GraphQLDescription("Describes the stage that is providing this report")
    var stageInfo: StageInfo? = null,

    @GraphQLDescription("Optional tag(s) associated with this report")
    var tags: Map<String,String>? = null,

    @GraphQLDescription("Optional data associated with this report")
    var data: Map<String,String>? = null,

    @GraphQLDescription("Indicates the content type of the content; e.g. JSON, XML")
    var contentType : String? = null,

    @GraphQLDescription("Jurisdiction report belongs to; set to null if not applicable")
    var jurisdiction: String? = null,

    @GraphQLDescription("SenderId this report belongs to; set to null if not applicable")
    var senderId: String? = null,

    @GraphQLDescription("Content of the report.  If the report is JSON then the content will be shown as JSON.  Otherwise, the content is a base64 encoded string.")
    var content : Map<*, *>? = null,

    @GraphQLDescription("Date/time of when the report was recorded in the database")
    var timestamp: OffsetDateTime? = null,

    @GraphQLDescription("Disposition type of the report")
    var dispositionType: String? = null,

    @GraphQLDescription("List of reasons the report was sent to dead-letter")
    var deadLetterReasons: List<String>? = null,

    @GraphQLDescription("Schemas used to validate the report")
    var validationSchemas: List<String>? = null,
)