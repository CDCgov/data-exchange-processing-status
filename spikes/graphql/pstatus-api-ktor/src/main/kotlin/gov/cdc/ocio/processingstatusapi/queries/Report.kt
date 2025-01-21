package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDataLoader
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.ReportLoader
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletableFuture

class ReportQueryService : Query {

    @GraphQLDescription("Return a single report from the provided uploadId")
    @Suppress("unused")
    fun getReport(uploadId: String) = ReportLoader().getByUploadId(uploadId)

}

