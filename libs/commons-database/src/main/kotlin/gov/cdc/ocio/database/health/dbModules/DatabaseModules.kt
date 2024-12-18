package gov.cdc.ocio.database.health.dbModules

import gov.cdc.ocio.database.cosmos.CosmosConfiguration
import gov.cdc.ocio.database.couchbase.CouchbaseConfiguration
import gov.cdc.ocio.database.health.HealthCheckCosmosDb
import gov.cdc.ocio.database.health.HealthCheckCouchbaseDb
import gov.cdc.ocio.database.health.HealthCheckDynamoDb
import gov.cdc.ocio.database.health.HealthCheckMongoDb
import gov.cdc.ocio.database.health.dbClientFactory.CosmosDbClientFactory
import gov.cdc.ocio.database.health.dbClientFactory.CouchbaseClusterFactory
import gov.cdc.ocio.database.health.dbClientFactory.DynamoDbClientFactory
import gov.cdc.ocio.database.health.dbClientFactory.MongoDbClientFactory
import gov.cdc.ocio.database.mongo.MongoConfiguration
import org.koin.dsl.module

/**
 * Koin modules for database configurations and repositories
 */
object DatabaseModules {

    fun provideCosmosModule(uri: String, authKey: String) = module {
        single { CosmosConfiguration(uri, authKey) }
        single { CosmosDbClientFactory.createClient(uri, authKey) } // Create CosmosClient using the factory
        single { HealthCheckCosmosDb(get()) } // Inject CosmosClient and CosmosConfiguration
    }

    fun provideCouchbaseModule(connectionString: String, username: String, password: String) = module {
        single { CouchbaseConfiguration(connectionString, username, password) }
        single { CouchbaseClusterFactory.createCluster(get()) } // Create Cluster using the factory
        single { HealthCheckCouchbaseDb(get()) } // Inject Cluster into the health check
    }

    fun provideDynamoModule(roleArn: String?, webIdentityTokenFile: String? ) = module {
        single {
            DynamoDbClientFactory.createClient(
                roleArn,
                webIdentityTokenFile
            )
        }

        // Inject DynamoDB client into the health check
        single { HealthCheckDynamoDb(get()) }
    }

    // MongoDB Module
    fun provideMongoModule(connectionString: String, databaseName: String) = module {
        single { MongoConfiguration(connectionString, databaseName) }
        single { MongoDbClientFactory.createClient(get()) } // Create MongoDB client using the factory
        single { HealthCheckMongoDb(get(),get()) } // HealthCheckMongoDb depends on MongoConfiguration injected via Koin
    }
}


