package gov.cdc.ocio.processingstatusapi.models.reports

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.models.submission.Aggregation
import gov.cdc.ocio.processingstatusapi.models.submission.Status
import java.time.OffsetDateTime

/**
 * Sealed class representing different types of content.
 */
/*@GraphQLDescription("Content can be either JSON or a base64 encoded string.")
//@GraphQLUnion(name = "ContentInput", possibleTypes = ["JsonContentInput", "StringContentInput"])
sealed class ContentInput

@GraphQLDescription("Input type for JSON content")
data class JsonContentInput(
    @GraphQLDescription("JSON data")
    val data: Map<String, Any>
) : ContentInput()

@GraphQLDescription("Input type for Base64 encoded string content")
data class StringContentInput(
    @GraphQLDescription("Base64 encoded string")
    val content: String
) : ContentInput()*/

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

    @GraphQLDescription("Indicates the content type of the content; e.g. JSON, XML")
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

@GraphQLDescription("Input type for stage info")
data class StageInfoInput(
    @GraphQLDescription("Service")
    val service: String? = null,

    @GraphQLDescription("Stage name a.k.a action")
    val action: String? = null,

    @GraphQLDescription("Version")
    val version: String? = null,

    @GraphQLDescription("Status- SUCCESS OR FAILURE")
     val status: Status? = null,

    @GraphQLDescription("Issues array")
    val issues: List<IssueInput>? = null,

    @GraphQLDescription("Start processing time")
    val startProcessingTime: OffsetDateTime? = null,

    @GraphQLDescription("End processing time")
    val endProcessingTime: OffsetDateTime? = null
)

@GraphQLDescription("Input type for issues")
data class IssueInput(
    @GraphQLDescription("Issue code")
    val code: String? = null,

    @GraphQLDescription("Issue description")
    val description: String? = null
)


@GraphQLDescription("Input type for tags")
data class TagInput(
    @GraphQLDescription("Tag key")
    val key: String,

    @GraphQLDescription("Tag value")
    val value: String
)

@GraphQLDescription("Input type for tags")
data class DataInput(
    @GraphQLDescription("Tag key")
    val key: String,

    @GraphQLDescription("Tag value")
    val value: String
)