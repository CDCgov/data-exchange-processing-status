package gov.cdc.ocio.processingstatusapi.models.reports.inputs

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import java.time.OffsetDateTime

@GraphQLDescription("Input type for creating or updating a report")
data class ReportInput(
    @GraphQLDescription("Identifier of the report recorded by the database")
    val id: String? = null,

    @GraphQLDescription("Upload identifier this report belongs to")
    val uploadId: String? = null,

    @GraphQLDescription("Unique report identifier")
    val reportId: String? = null,

    @GraphQLDescription("Data stream ID")
    val dataStreamId: String? = null,

    @GraphQLDescription("Data stream route")
    val dataStreamRoute: String? = null,

    @GraphQLDescription("Date/time of when the upload was first ingested into the data-exchange")
    val dexIngestDateTime: OffsetDateTime? = null,

    @GraphQLDescription("Message metadata")
    val messageMetadata: MessageMetadataInput? = null,

    @GraphQLDescription("Stage info")
    val stageInfo: StageInfoInput? = null,

    @GraphQLDescription("Tags")
    val tags: List<TagInput>? = null,

    @GraphQLDescription("Data")
    val data: List<DataInput>? = null,

    @GraphQLDescription("Indicates the content type of the content; e.g. JSON, APPLICATION/JSON, XML, BASE64")
    val contentType: String? = null,

    @GraphQLDescription("Jurisdiction report belongs to; set to null if not applicable")
    val jurisdiction: String? = null,

    @GraphQLDescription("Sender ID this report belongs to; set to null if not applicable")
    val senderId: String? = null,

    @GraphQLDescription("Data Producer ID stated in the report; set to null if not applicable")
    val dataProducerId: String? = null,

    @GraphQLDescription("Content of the report. If the report is JSON then the content will be a map, otherwise, it will be a string")
    var content : String? = null,

    @GraphQLDescription("Timestamp when the report was recorded in the database")
    val timestamp: OffsetDateTime? = null
)