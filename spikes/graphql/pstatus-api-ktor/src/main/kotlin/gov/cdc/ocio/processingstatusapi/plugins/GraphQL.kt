package gov.cdc.ocio.processingstatusapi.plugins

import com.expediagroup.graphql.dataloader.KotlinDataLoaderRegistryFactory
import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDataLoader
import gov.cdc.ocio.processingstatusapi.queries.ReportQueryService
import io.ktor.server.application.*
import io.ktor.server.routing.*

class HelloWorldQuery : Query {
    fun hello(): String = "Hello World!"
}
fun Application.graphQLModule() {
    install(GraphQL) {
        schema {
            packages = listOf("gov.cdc.ocio.processingstatusapi")
            queries = listOf(
                HelloWorldQuery(),
                ReportQueryService()
            )
        }
        engine {
            dataLoaderRegistryFactory = KotlinDataLoaderRegistryFactory(
                ReportDataLoader
            )
        }
    }
    install(Routing) {
        graphQLPostRoute()
        graphiQLRoute() // Go to http://localhost:8080/graphiql for the GraphQL playground
    }
}