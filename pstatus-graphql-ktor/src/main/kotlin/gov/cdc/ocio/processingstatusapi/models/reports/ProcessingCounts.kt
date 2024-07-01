package gov.cdc.ocio.processingstatusapi.models.reports

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Provides file processing counts")
data class ProcessingCounts(

    @GraphQLDescription("Total number of files found matching the search criteria")
    var totalCounts: Long = 0,

    @GraphQLDescription("Status counts for the files found matching the search criteria")
    var statusCounts: StatusCounts = StatusCounts()
)

@GraphQLDescription("Counts by current state")
data class StatusCounts(
    @GraphQLDescription("Number of files in the process of uploading")
    var uploading: CountsDetails = CountsDetails(),

    @GraphQLDescription("Number of files that failed upload")
    var failed: CountsDetails = CountsDetails(),

    @GraphQLDescription("Number of files that were successfully uploaded")
    var uploaded: CountsDetails = CountsDetails()
)

@GraphQLDescription("Structure containing the count details")
data class CountsDetails(

    @GraphQLDescription("Count of files")
    var counts: Long = 0,

    @GraphQLDescription("If a file failed to upload, this contains a list of reasons why it may have failed and the count for each reason type")
    var reasons: Map<String, Long>? = null
)
