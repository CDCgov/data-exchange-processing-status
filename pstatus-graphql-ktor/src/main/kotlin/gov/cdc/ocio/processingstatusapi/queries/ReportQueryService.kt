package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.loaders.ReportLoader
import gov.cdc.ocio.processingstatusapi.models.SortOrder
import graphql.schema.DataFetchingEnvironment

class ReportQueryService : Query {


    /**
     *  Submission details contain all the known details for a particular upload.
     *  It provides a roll-up of all the reports associated with the upload as well as some summary information.
     *
     * @param dataFetchingEnvironment DataFetchingEnvironment
     * @param uploadId String
     * @param reportsSortedBy String?
     * @param sortOrder SortOrder?
     * @return List<Report>
     */
    @GraphQLDescription("Returns all the reports associated with the provided upload ID.")
    @Suppress("unused")
    fun getReports(dataFetchingEnvironment: DataFetchingEnvironment,
                   @GraphQLDescription("Upload ID to retrieve all the reports for.")
                   uploadId: String,
                   @GraphQLDescription("Optional field to specify the field reports should be sorted by.  Available fields for sorting are: [`timestamp`].")
                   reportsSortedBy: String?,
                   @GraphQLDescription("Optional sort order.  When `reportsSortedBy` is provided, the available options are `Ascending` or `Descending`, which defaults to `Ascending` if not provided.")
                   sortOrder: SortOrder?) = ReportLoader()
        .getByUploadId(
            dataFetchingEnvironment,
            uploadId,
            reportsSortedBy,
            sortOrder
        )

    /**
     * Provides submission details for a particular upload.  It provides a roll-up of all the reports associated
     * with the upload as well as some summary information.
     *
     * @param dataFetchingEnvironment DataFetchingEnvironment
     * @param uploadId String
     * @param reportsSortedBy String?
     * @param sortOrder SortOrder?
     * @return SubmissionDetails
     */
    @GraphQLDescription("Returns the submission details for the provided upload ID.")
    @Suppress("unused")
    fun getSubmissionDetails(dataFetchingEnvironment: DataFetchingEnvironment,
                             @GraphQLDescription("Upload ID to retrieve the submission details for.")
                             uploadId: String,
                             @GraphQLDescription("Optional field to specify the field reports should be sorted by.  Available fields for sorting are: [`timestamp`].")
                             reportsSortedBy: String?,
                             @GraphQLDescription("Optional sort order.  When `reportsSortedBy` is provided, the available options are `Ascending` or `Descending`, which defaults to `Ascending` if not provided.")
                             sortOrder: SortOrder?) = ReportLoader()
        .getSubmissionDetailsByUploadId(
            dataFetchingEnvironment,
            uploadId,
            reportsSortedBy,
            sortOrder
        )


}

