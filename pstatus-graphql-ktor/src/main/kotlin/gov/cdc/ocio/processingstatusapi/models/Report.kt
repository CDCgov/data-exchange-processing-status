package gov.cdc.ocio.processingstatusapi.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.models.submission.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.submission.StageInfo
import java.time.OffsetDateTime

/**
 * Report for a given stage.
 *
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property messageMetadata MessageMetadata?
 * @property stageInfo StageInfo?
 * @property tags Tags?
 * @property data Map<String,String>?
 * @property contentType String?
 * @property messageId String?
 * @property jurisdiction String?
 * @property senderId String?
 * @property content String?
 * @property timestamp OffsetDateTime
 */
@GraphQLDescription("Contains Report content.")
data class Report(

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

    @GraphQLDescription("Data stream route")
    var dexIngestDateTime: OffsetDateTime? = null,

    @GraphQLDescription("Message metadata")
    var messageMetadata: MessageMetadata? = null,

    @GraphQLDescription("Stage info")
    var stageInfo: StageInfo? = null,

    @GraphQLDescription("tags")
    var tags: Map<String,String>? = null,

    @GraphQLDescription("data")
    var data: Map<String,String>? = null,

    @GraphQLDescription("Indicates the content type of the content; e.g. JSON, XML")
    var contentType : String? = null,

    @GraphQLDescription("Message id this report belongs to; set to null if not applicable")
    var messageId: String? = null,

    @GraphQLDescription("Jurisdiction report belongs to; set to null if not applicable")
    var jurisdiction: String? = null,

    @GraphQLDescription("SenderId this report belongs to; set to null if not applicable")
    var senderId: String? = null,

    @GraphQLDescription("Content of the report.  If the report is JSON then the content will be shown as JSON.  Otherwise, the content is a base64 encoded string.")
    var content : Map<*, *>? = null,

    @GraphQLDescription("Datestamp the report was recorded in the database")
    var timestamp: OffsetDateTime? = null
)