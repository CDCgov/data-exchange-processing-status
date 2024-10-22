package gov.cdc.ocio.processingstatusapi.models.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Collection of various uploads statistics")
data class UploadStats(

    @GraphQLDescription("The total number of unique upload ids found for that day.  However, this does not mean that all those are successful uploads.  This will include any uploads that fail out due to retries on client side.")
    var uniqueUploadIdsCount: Long = 0,

    @GraphQLDescription("Number of upload ids that made it past the metadata verify step; these are uploads that actually reported at least one status update.")
    var uploadsWithStatusCount: Long = 0,

    @GraphQLDescription("Total number of uploads that were stopped by the upload api due to one or more issues with the metadata received.")
    var badMetadataCount: Long = 0,

    @GraphQLDescription("Number of uploads that have been completed.  This means, not only did the upload start, but according to the upload status reports we have received 100% of the expected chunks.")
    var completedUploadsCount: Long = 0,

    @GraphQLDescription("Provides a list of all the duplicate filenames that were uploaded and how many.")
    var duplicateFilenames: List<DuplicateFilenameCounts> = listOf(),

    @GraphQLDescription(
        "Provides a list of all the uploads that have not been delivered. \n" +
        "Any upload is considered undelivered when: \n" +
        "'1. ' Any upload id where an upload-completed report exists, but not a blob-file-copy report. \n" +
        "'2. ' Any upload id where a blob-file-copy report exists and it's status indicates failure. \n"
    )
    var undeliveredUploads: UndeliveredUploadCounts = UndeliveredUploadCounts(),

    @GraphQLDescription(
        "Provides a list of all the uploads that are pending. \n" +
        "Any upload id where a metadata-verify report exists but not a report with upload-completed."
    )
    var pendingUploads: PendingUploadCounts = PendingUploadCounts()
)
