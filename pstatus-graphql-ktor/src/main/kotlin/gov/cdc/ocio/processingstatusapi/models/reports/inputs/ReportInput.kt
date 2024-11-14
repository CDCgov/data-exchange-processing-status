package gov.cdc.ocio.processingstatusapi.models.reports.inputs

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.google.gson.annotations.SerializedName
import java.time.OffsetDateTime

@GraphQLDescription("Input type for creating or updating a report")
data class ReportInput(
    @GraphQLDescription("Identifier of the report recorded by the database")
    @SerializedName("id")
    val id: String? = null,

    @GraphQLDescription("Upload identifier this report belongs to")
    @SerializedName("upload_id")
    val uploadId: String? = null,

    @GraphQLDescription("Unique report identifier")
    @SerializedName("report_id")
    val reportId: String? = null,

    @GraphQLDescription("Data stream ID")
    @SerializedName("data_stream_id")
    val dataStreamId: String? = null,

    @GraphQLDescription("Data stream route")
    @SerializedName("data_stream_route")
    val dataStreamRoute: String? = null,

    @GraphQLDescription("Date/time of when the upload was first ingested into the data-exchange")
    @SerializedName("dex_ingest_datetime")
    val dexIngestDateTime: OffsetDateTime? = null,

    @GraphQLDescription("Message metadata")
    @SerializedName("message_metadata")
    val messageMetadata: MessageMetadataInput? = null,

    @GraphQLDescription("Stage info")
    @SerializedName("stage_info")
    val stageInfo: StageInfoInput? = null,

    @GraphQLDescription("Tags")
    @SerializedName("tags")
    val tags: Map<String,String>? = null,

    @GraphQLDescription("Data")
    @SerializedName("data")
    val data: Map<String,String>? = null,

    @GraphQLDescription("Indicates the content type of the content; e.g. JSON, APPLICATION/JSON, XML, BASE64")
    @SerializedName("content_type")
    val contentType: String? = null,

    @GraphQLDescription("Jurisdiction report belongs to; set to null if not applicable")
    @SerializedName("jurisdiction")
    val jurisdiction: String? = null,

    @GraphQLDescription("Sender ID this report belongs to; set to null if not applicable")
    @SerializedName("sender_id")
    val senderId: String? = null,

    @GraphQLDescription("Data Producer ID stated in the report; set to null if not applicable")
    @SerializedName("data_producer_id")
    val dataProducerId: String? = null,

    @GraphQLDescription("Content of the report. If the report is JSON then the content will be a map, otherwise, it will be a string")
    var content : String? = null,

    @GraphQLDescription("Timestamp when the report was recorded in the database")
    @SerializedName("timestamp")
    val timestamp: OffsetDateTime? = null
)