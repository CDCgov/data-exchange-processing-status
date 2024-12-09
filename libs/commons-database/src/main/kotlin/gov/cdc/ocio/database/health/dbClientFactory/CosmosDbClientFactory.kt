package gov.cdc.ocio.database.health.dbClientFactory

import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosClientBuilder

/**
 * Factory for creating and managing Cosmos DB clients.
 */
object CosmosDbClientFactory {
    fun createClient(uri: String, authKey: String): CosmosClient {
        return CosmosClientBuilder()
            .endpoint(uri)
            .key(authKey)
            .gatewayMode()
            //.preferredRegions(listOf("East US")) // Optional: specify preferred regions
            .consistencyLevel(com.azure.cosmos.ConsistencyLevel.SESSION) // Example consistency level
            .buildClient()
    }
}
