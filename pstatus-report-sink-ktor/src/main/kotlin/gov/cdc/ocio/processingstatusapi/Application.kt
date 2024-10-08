package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.database.DatabaseType
import gov.cdc.ocio.database.cosmos.CosmosConfiguration
import gov.cdc.ocio.database.cosmos.CosmosRepository
import gov.cdc.ocio.database.couchbase.CouchbaseConfiguration
import gov.cdc.ocio.database.couchbase.CouchbaseRepository
import gov.cdc.ocio.database.dynamo.DynamoRepository
import gov.cdc.ocio.database.mongo.MongoConfiguration
import gov.cdc.ocio.database.mongo.MongoRepository
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.plugins.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import mu.KotlinLogging
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

enum class MessageSystem {
    AWS,
    AZURE_SERVICE_BUS,
    RABBITMQ
}

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
        val database = environment.config.property("ktor.database").getString()
        var databaseType = DatabaseType.UNKNOWN

        when (database.lowercase()) {
            DatabaseType.MONGO.value -> {
                val connectionString = environment.config.property("mongo.connection_string").getString()
                val databaseName = environment.config.property("mongo.database_name").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    MongoRepository(connectionString, databaseName)
                }

                //  Create a MongoDB config that can be dependency injected (for health checks)
                single { MongoConfiguration(connectionString, databaseName) }
                databaseType = DatabaseType.MONGO
            }
            DatabaseType.COUCHBASE.value -> {
                val connectionString = environment.config.property("couchbase.connection_string").getString()
                val username = environment.config.property("couchbase.username").getString()
                val password = environment.config.property("couchbase.password").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    CouchbaseRepository(connectionString, username, password)
                }

                //  Create a couchbase config that can be dependency injected (for health checks)
                single { CouchbaseConfiguration(connectionString, username, password) }
                databaseType = DatabaseType.COUCHBASE
            }
            DatabaseType.COSMOS.value -> {
                val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
                val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    CosmosRepository(uri, authKey, "/uploadId")
                }

                //  Create a CosmosDB config that can be dependency injected (for health checks)
                single { CosmosConfiguration(uri, authKey) }
                databaseType = DatabaseType.COSMOS
            }
            DatabaseType.DYNAMO.value -> {
                val dynamoTablePrefix = environment.config.property("aws.dynamo.table_prefix").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    DynamoRepository(dynamoTablePrefix)
                }
                databaseType = DatabaseType.DYNAMO
            }
            else -> logger.error("Unsupported database requested: $databaseType")
        }
        single { databaseType } // add databaseType to Koin Modules
    }

    val messageSystemModule = module {
        val msgType = environment.config.property("ktor.message_system").getString()
        single {msgType} // add msgType to Koin Modules

        when (msgType) {
            MessageSystem.AZURE_SERVICE_BUS.toString() -> {
                single(createdAtStart = true) {
                    AzureServiceBusConfiguration(environment.config, configurationPath = "azure.service_bus") }

            }
            MessageSystem.RABBITMQ.toString() -> {
                single(createdAtStart = true) {
                    RabbitMQServiceConfiguration(environment.config, configurationPath = "rabbitMQ")
                }

            }
            MessageSystem.AWS.toString() -> {
                single(createdAtStart = true) {
                    AWSSQServiceConfiguration(environment.config, configurationPath = "aws")
                }
            }
        }
    }

    return modules(listOf(databaseModule, messageSystemModule))
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

    //determine which messaging system module to load
    val messageSystem: MessageSystem? =  try {
        val currentMessagingSystem = environment.config.property("ktor.message_system").getString()
        MessageSystem.valueOf(currentMessagingSystem)
    } catch (e: IllegalArgumentException) {
        log.error("Invalid message system configuration: ${e.message}")
        null
    }

    when (messageSystem) {
        MessageSystem.AZURE_SERVICE_BUS -> {
            serviceBusModule()
        }
        MessageSystem.RABBITMQ -> {
            rabbitMQModule()
        }

        MessageSystem.AWS -> {
            awsSQSModule()
        }
        else -> log.error("Invalid message system configuration")
    }

    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
    }
}
