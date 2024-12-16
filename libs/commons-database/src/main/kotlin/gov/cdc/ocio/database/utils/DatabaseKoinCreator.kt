package gov.cdc.ocio.database.utils

import gov.cdc.ocio.database.DatabaseType
import gov.cdc.ocio.database.cosmos.CosmosConfiguration
import gov.cdc.ocio.database.cosmos.CosmosRepository
import gov.cdc.ocio.database.couchbase.CouchbaseConfiguration
import gov.cdc.ocio.database.couchbase.CouchbaseRepository
import gov.cdc.ocio.database.dynamo.DynamoRepository
import gov.cdc.ocio.database.health.dbModules.DatabaseModules
import gov.cdc.ocio.database.mongo.MongoConfiguration
import gov.cdc.ocio.database.mongo.MongoRepository
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import io.ktor.server.application.*
import io.ktor.server.config.*
import mu.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module


/**
 * Helper class for creating koin modules for a database.
 */
class DatabaseKoinCreator {

    companion object {

        /**
         * Creates a koin module and injects singletons for the database config specified in the [ApplicationEnvironment]
         *
         * @param environment [ApplicationEnvironment] Application environment to obtain the database parameters from.
         * @return [Module] Resultant koin module.
         */
        fun moduleFromAppEnv(environment: ApplicationEnvironment): Module {
            val logger = KotlinLogging.logger {}

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
                        val roleArn = environment.config.tryGetString("aws.role_arn") ?: ""
                        val webIdentityTokenFile = environment.config.tryGetString("aws.web_identity_token_file")?: ""
                        single<ProcessingStatusRepository>(createdAtStart = true) {
                            DynamoRepository(dynamoTablePrefix,roleArn,webIdentityTokenFile)
                        }
                        databaseType = DatabaseType.DYNAMO
                    }

                    else -> logger.error("Unsupported database requested: $databaseType")
                }
                single { databaseType } // add databaseType to Koin Modules
            }
            return databaseModule
        }

        /**
         *  Set the appropriate database module
         *  Creates a koin module and injects singletons for the database config specified in the [ApplicationEnvironment]
         *  @param environment ApplicationEnvironment
         * @return [Module] Resultant koin module.
         */
        fun dbHealthCheckModuleFromAppEnv(environment: ApplicationEnvironment):Module {
            val logger = KotlinLogging.logger {}
            val database = environment.config.property("ktor.database").getString()
            val databaseType = DatabaseType.UNKNOWN
            when (database.lowercase()) {
                DatabaseType.COSMOS.value ->
                    return  DatabaseModules.provideCosmosModule(
                        uri = environment.config.property("azure.cosmos_db.client.endpoint").getString(),
                        authKey = environment.config.property("azure.cosmos_db.client.key").getString()
                    )
                DatabaseType.COUCHBASE.value -> return DatabaseModules.provideCouchbaseModule(
                    connectionString = environment.config.property("couchbase.connection_string").getString(),
                    username = environment.config.property("couchbase.username").getString(),
                    password = environment.config.property("couchbase.password").getString()
                )
                DatabaseType.DYNAMO.value ->return DatabaseModules.provideDynamoModule(
                   roleArn = environment.config.tryGetString("aws.role_arn") ?: "",
                   webIdentityTokenFile =environment.config.tryGetString("aws.web_identity_token_file") ?: ""
                )
                DatabaseType.MONGO.value ->return DatabaseModules.provideMongoModule(
                    connectionString = environment.config.property("mongo.connection_string").getString(),
                    databaseName = environment.config.property("mongo.database_name").getString()
                )
                else -> logger.error("Unsupported database requested: $databaseType")
            }
            return module { single { DatabaseType.UNKNOWN } }
        }
    }
}