package gov.cdc.ocio.processingstatusapi.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.loaders.ReportLoader
import kotlinx.coroutines.runBlocking
import graphql.GraphQLContext
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import java.util.concurrent.CompletableFuture

val ReportDataLoader = object : KotlinDataLoader<String, Report> {
    override val dataLoaderName = "REPORTS_LOADER"
    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<String, Report> =
        DataLoaderFactory.newDataLoader { ids ->
            CompletableFuture.supplyAsync {
                runBlocking { ReportLoader().search(ids).toMutableList() }
            }
        }
}
