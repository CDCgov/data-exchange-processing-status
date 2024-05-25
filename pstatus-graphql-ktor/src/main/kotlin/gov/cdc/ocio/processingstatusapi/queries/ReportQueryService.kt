package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDataLoader
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.loaders.ReportLoader
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletableFuture

class ReportQueryService : Query {

    @GraphQLDescription("Return all the reports associated with the provided uploadId")
    @Suppress("unused")
    fun getReports(uploadId: String) = ReportLoader().getByUploadId(uploadId)

    @GraphQLDescription("Return list of reports based on ReportSearchParameters options")
    @Suppress("unused")
    fun searchReports(params: ReportSearchParameters, dfe: DataFetchingEnvironment): CompletableFuture<List<Report>> =
        dfe.getDataLoader<String, Report>(ReportDataLoader.dataLoaderName)
            .loadMany(params.ids)
}

data class ReportSearchParameters(val ids: List<String>)