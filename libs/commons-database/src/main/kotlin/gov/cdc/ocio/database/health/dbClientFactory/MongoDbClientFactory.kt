package gov.cdc.ocio.database.health.dbClientFactory

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoClient
import gov.cdc.ocio.database.mongo.MongoConfiguration

object MongoDbClientFactory {

    /**
     * Creates a MongoClient instance based on the provided configuration.
     *
     * @param config MongoConfiguration
     * @return MongoClient
     */
    fun createClient(config: MongoConfiguration): MongoClient {
        val serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build()

        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(config.connectionString))
            .serverApi(serverApi)
            .build()

        return MongoClients.create(settings)
    }
}
