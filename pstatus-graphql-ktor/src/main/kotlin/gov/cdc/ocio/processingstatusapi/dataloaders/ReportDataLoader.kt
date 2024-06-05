package gov.cdc.ocio.processingstatusapi.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import gov.cdc.ocio.processingstatusapi.loaders.ReportDeadLetterLoader
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.loaders.ReportLoader
import gov.cdc.ocio.processingstatusapi.models.ReportDeadLetter
import kotlinx.coroutines.runBlocking
import graphql.GraphQLContext
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import java.util.concurrent.CompletableFuture.*

val ReportDataLoader = object : KotlinDataLoader<String, Report> {
    override val dataLoaderName = "REPORTS_LOADER"
    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<String, Report> =
        DataLoaderFactory.newDataLoader { ids ->
            supplyAsync {
                runBlocking { ReportLoader().search(ids).toMutableList() }
            }
        }
}


val ReportDeadLetterDataLoader = object : KotlinDataLoader<String, ReportDeadLetter> {
    override val dataLoaderName = "REPORTS_DEAD_LETTER_LOADER"
    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<String, ReportDeadLetter> =
        DataLoaderFactory.newDataLoader { ids ->
            supplyAsync {
                runBlocking { ReportDeadLetterLoader().search(ids).toMutableList() }
            }
        }
}