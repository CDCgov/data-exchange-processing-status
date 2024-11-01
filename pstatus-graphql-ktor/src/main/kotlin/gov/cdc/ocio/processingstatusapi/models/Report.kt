package gov.cdc.ocio.processingstatusapi.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.database.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.submission.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.submission.StageInfo
import java.time.OffsetDateTime
import java.time.ZoneOffset


/**
 * Report for a given stage.
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
 * @property dataProducerId String?
 * @property content Map<*, *>?
 * @property timestamp OffsetDateTime?
 * @constructor
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

    @GraphQLDescription("Date/time of when the upload was first ingested into the data-exchange")
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

    @GraphQLDescription("Jurisdiction report belongs to; set to null if not applicable")
    var jurisdiction: String? = null,

    @GraphQLDescription("SenderId this report belongs to; set to null if not applicable")
    var senderId: String? = null,

    @GraphQLDescription("DataProducerId stated in the report; set to null if not applicable")
    var dataProducerId: String? = null,

    @GraphQLDescription("Content of the report.  If the report is JSON then the content will be shown as JSON.  Otherwise, the content is a base64 encoded string.")
    var content : Map<*, *>? = null,

    @GraphQLDescription("Datestamp the report was recorded in the database")
    var timestamp: OffsetDateTime? = null
) {
    companion object {
        /**
         * Convenience function to convert a cosmos data object to a Report object
         */
        fun fromReportDao(dao: ReportDao) = Report().apply {
            this.id = dao.id
            this.uploadId = dao.uploadId
            this.reportId = dao.reportId
            this.dataStreamId = dao.dataStreamId
            this.dataStreamRoute = dao.dataStreamRoute
            this.dexIngestDateTime = dao.dexIngestDateTime?.atOffset(ZoneOffset.UTC)
            this.messageMetadata = MessageMetadata.fromMessageMetadataDao(dao.messageMetadata)
            this.stageInfo = StageInfo.fromStageInfoDao(dao.stageInfo)
            this.tags = dao.tags
            this.data = dao.data
            this.jurisdiction = dao.jurisdiction
            this.senderId = dao.senderId
            this.dataProducerId = dao.dataProducerId
            this.timestamp = dao.timestamp?.atOffset(ZoneOffset.UTC)
            this.contentType = dao.contentType
            this.content = dao.contentAsMap
        }
    }
}