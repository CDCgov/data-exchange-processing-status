package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.processingstatusapi.cosmos.CosmosConfiguration
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.couchbase.CouchbaseRepository
import gov.cdc.ocio.processingstatusapi.mongo.MongoRepository
import gov.cdc.ocio.processingstatusapi.mongo.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.plugins.AzureServiceBusConfiguration
import gov.cdc.ocio.processingstatusapi.plugins.configureRouting
import gov.cdc.ocio.processingstatusapi.plugins.serviceBusModule
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

private val logger = KotlinLogging.logger {}


/**
 * Load the environment configuration values
 *
 * @receiver KoinApplication
 * @param environment ApplicationEnvironment
 * @return KoinApplication
 */
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val databaseModule = module {
        when (val database = environment.config.tryGetString("ktor.database")) {
            DatabaseType.MONGO.value -> {
                val connectionString = environment.config.property("mongo.connection_string").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    MongoRepository(connectionString, "ProcessingStatus")
                }
            }
            DatabaseType.COUCHBASE.value -> {
                val connectionString = environment.config.property("couchbase.connection_string").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    CouchbaseRepository(connectionString, "admin", "password")
                }
            }
            DatabaseType.COSMOS.value -> {
                val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
                val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    CosmosRepository(uri, authKey, "Reports", "/uploadId")
                }

                //  Create a CosmosDB config that can be dependency injected (for health checks)
                single(createdAtStart = true) { CosmosConfiguration(uri, authKey) }
            }
            else -> logger.error("Unsupported database requested: $database")
        }
    }
    val asbConfigModule = module {
        // Create an azure service bus config that can be dependency injected (for health checks)
        single(createdAtStart = true) { AzureServiceBusConfiguration(environment.config, configurationPath = "azure.service_bus") }
    }

    return modules(listOf(databaseModule, asbConfigModule))
}

/**
 * The main function
 *  @param args Array<string>
 */
fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

/**
 * The main application module which always runs and loads other modules
 */
fun Application.module() {
    configureRouting()
    serviceBusModule()
    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
    }
}
