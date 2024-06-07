package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDeadLetterDataLoader
import gov.cdc.ocio.processingstatusapi.loaders.ReportDeadLetterLoader
import gov.cdc.ocio.processingstatusapi.models.ReportDeadLetter
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletableFuture

class ReportDeadLetterQueryService : Query {

    @GraphQLDescription("Return all the dead-letter reports associated with the provided uploadId")
    @Suppress("unused")
    fun getDeadLetterReportsByUploadId(uploadId: String) = ReportDeadLetterLoader().getByUploadId(uploadId)

    @GraphQLDescription("Return all the dead-letter reports associated with the provided datastreamId, datastreamroute and timestamp date range")
    @Suppress("unused")
    fun getDeadLetterReportsByDataStream(dataStreamId: String, dataStreamRoute:String, startDate:String, endDate:String)
              = ReportDeadLetterLoader().getByDataStreamByDateRange(dataStreamId,dataStreamRoute,startDate,endDate)

    @GraphQLDescription("Return count of dead-letter reports associated with the provided datastreamId, (optional) datastreamroute and timestamp date range")
    @Suppress("unused")
    fun getDeadLetterReportsCountByDataStream(dataStreamId: String, dataStreamRoute:String?, startDate:String, endDate:String)
            = ReportDeadLetterLoader().getCountByDataStreamByDateRange(dataStreamId,dataStreamRoute,startDate,endDate)

    @GraphQLDescription("Return list of dead-letter reports based on ReportSearchParameters options")
    @Suppress("unused")
    fun searchDeadLetterReports(params: ReportDeadLetterSearchParameters, dfe: DataFetchingEnvironment): CompletableFuture<List<ReportDeadLetter>> =
        dfe.getDataLoader<String, ReportDeadLetter>(ReportDeadLetterDataLoader.dataLoaderName)
            .loadMany(params.ids)
}

@GraphQLDescription("Parameters for searching for reports")
data class ReportDeadLetterSearchParameters(
    @GraphQLDescription("Array of report IDs to search for and retrieve")
    val ids: List<String>
)