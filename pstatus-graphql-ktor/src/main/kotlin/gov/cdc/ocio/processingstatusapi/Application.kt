package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.database.DatabaseType
import gov.cdc.ocio.database.cosmos.CosmosRepository
import gov.cdc.ocio.database.couchbase.CouchbaseRepository
import gov.cdc.ocio.database.dynamo.DynamoRepository
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.plugins.configureRouting
import gov.cdc.ocio.processingstatusapi.plugins.graphQLModule
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import mu.KotlinLogging
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val logger = KotlinLogging.logger {}
    val databaseModule = module {
        when (val database = environment.config.tryGetString("ktor.database")) {
            DatabaseType.COUCHBASE.value -> {
                val connectionString = environment.config.property("couchbase.connection_string").getString()
                single<ProcessingStatusRepository> {
                    CouchbaseRepository(connectionString, "admin", "password")
                }
            }
            DatabaseType.COSMOS.value -> {
                val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
                val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
                single<ProcessingStatusRepository> {
                    CosmosRepository(uri, authKey, "/uploadId")
                }
            }
            DatabaseType.DYNAMO.value -> {
                val dynamoTablePrefix = environment.config.property("aws.dynamo.table_prefix").getString()
                single<ProcessingStatusRepository> {
                    DynamoRepository(dynamoTablePrefix)
                }
            }
            else -> logger.error("Unsupported database requested: $database")
        }
    }
    return modules(listOf(databaseModule))
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {

    install(Koin) {
        loadKoinModules(environment)
    }
    graphQLModule()
    configureRouting()

    install(ContentNegotiation) {
        jackson()
    }
    // See https://opensource.expediagroup.com/graphql-kotlin/docs/schema-generator/writing-schemas/scalars
    RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Date)
}
