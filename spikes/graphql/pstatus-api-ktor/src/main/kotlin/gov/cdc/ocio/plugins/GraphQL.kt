package gov.cdc.ocio.plugins

import com.expediagroup.graphql.dataloader.KotlinDataLoaderRegistryFactory
import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.dataloaders.ReportDataLoader
import gov.cdc.ocio.queries.ReportQueryService
import io.ktor.server.application.*
import io.ktor.server.routing.*

class HelloWorldQuery : Query {
    fun hello(): String = "Hello World!"
}
fun Application.graphQLModule() {
    install(GraphQL) {
        schema {
            packages = listOf("gov.cdc.ocio")
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