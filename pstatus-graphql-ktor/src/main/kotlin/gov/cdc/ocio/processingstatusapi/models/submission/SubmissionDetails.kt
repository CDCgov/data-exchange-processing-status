package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.models.Report
import java.time.OffsetDateTime

/**
 * Submission details for a given upload.
 *
 * @property status String?
 * @property lastService String?
 * @property lastAction String?
 * @property filename String?
 * @property uploadId String?
 * @property dexIngestDateTime  OffsetDateTime?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property jurisdiction String?
 * @property senderId String?
 * @property reports List<Report>
 */
@GraphQLDescription("Contains upload details")
data class SubmissionDetails(

    @GraphQLDescription("Rollup status [DELIVERED, FAILED, PROCESSING]")
    var status: String? = null,

    @GraphQLDescription("Find report with most recent timestamp for the upload ID and report the service.")
    var lastService: String? = null,

    @GraphQLDescription("Find report with most recent timestamp for the upload ID and report the action.")
    var lastAction: String? = null,

    @GraphQLDescription("Locate first found report with service, 'upload' and action 'upload-status' for the given upload ID and report the filename.")
    var filename: String? = null,

    @GraphQLDescription("Upload Id of the report")
    var uploadId: String? = null,

    @GraphQLDescription("Datestamp the report was recorded in the database")
    var dexIngestDateTime: OffsetDateTime? = null,

    @GraphQLDescription("DataStreamId in the report")
    var dataStreamId: String? = null,

    @GraphQLDescription("DataStreamRoute in the report")
    var dataStreamRoute: String? = null,

    @GraphQLDescription("Jurisdiction stated in the report")
    var jurisdiction: String? = null,

    @GraphQLDescription("SenderId mentioned in the report")
    var senderId: String? = null,

    @GraphQLDescription("Array of the raw reports provided for this upload ID.")
    var reports: List<Report>? = null
)
