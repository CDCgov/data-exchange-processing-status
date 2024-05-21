package gov.cdc.ocio.processingstatusapi.cosmos

import com.azure.cosmos.ConsistencyLevel
import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosClientBuilder

class CosmosClientManager {
    companion object {

        private var client: CosmosClient? = null

        fun getCosmosClient(uri: String, authKey: String): CosmosClient? {
            // Initialize a connection to cosmos that will persist across HTTP triggers
            if (client == null) {
                client = CosmosClientBuilder()
                    .endpoint(uri)
                    .key(authKey)
                    .consistencyLevel(ConsistencyLevel.EVENTUAL)
                    .contentResponseOnWriteEnabled(true)
                    .clientTelemetryEnabled(false)
                    .buildClient()
            }
            return client
        }
    }
}