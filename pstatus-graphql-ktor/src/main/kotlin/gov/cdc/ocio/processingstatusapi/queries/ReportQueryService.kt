package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDataLoader
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.loaders.ReportLoader
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletableFuture

class ReportQueryService : Query {

    /**
     * Query for retrieving reports for the uploadId provided.
     *
     * @param dataFetchingEnvironment DataFetchingEnvironment
     * @param uploadId String
     * @return List<Report>
     */
    @GraphQLDescription("Return all the reports associated with the provided uploadId")
    @Suppress("unused")
    fun getReports(dataFetchingEnvironment: DataFetchingEnvironment,
                   uploadId: String) = ReportLoader().getByUploadId(dataFetchingEnvironment, uploadId)

    /**
     * Searches for reports with the given search parameters.
     *
     * @param params ReportSearchParameters
     * @param dfe DataFetchingEnvironment
     * @return CompletableFuture<List<Report>>
     */
    @GraphQLDescription("Return list of reports based on ReportSearchParameters options")
    @Suppress("unused")
    fun searchReports(params: ReportSearchParameters, dfe: DataFetchingEnvironment): CompletableFuture<List<Report>> =
        dfe.getDataLoader<String, Report>(ReportDataLoader.dataLoaderName)
            .loadMany(params.ids)
}

/**
 * Report search parameters, which is a list of report IDs.
 *
 * @property ids List<String>
 * @constructor
 */
@GraphQLDescription("Parameters for searching for reports")
data class ReportSearchParameters(
    @GraphQLDescription("Array of report IDs to search for and retrieve")
    val ids: List<String>
)