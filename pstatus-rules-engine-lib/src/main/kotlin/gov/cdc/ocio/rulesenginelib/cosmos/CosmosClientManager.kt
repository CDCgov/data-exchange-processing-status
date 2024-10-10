package gov.cdc.ocio.rulesenginelib.gov.cdc.ocio.rulesenginelib.cosmos

import com.azure.cosmos.ConsistencyLevel
import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosClientBuilder
import kotlinx.coroutines.*
import java.time.Duration

class CosmosClientManager {

        private var client: CosmosClient? = null

        /**
         * Establishes a connection to the CosmosDB and returns a client
         *
         * @param uri String
         * @param authKey String
         * @return CosmosClient?
         */
        @OptIn(DelicateCoroutinesApi::class)
        fun getCosmosClient(uri: String, authKey: String): CosmosClient? {
            // Initialize a connection to cosmos that will persist across HTTP triggers
            if (client == null) {
                return try {
                    var d: Deferred<CosmosClient>? = null
                    GlobalScope.launch {
                        d = async {
                            CosmosClientBuilder()
                                .endpoint(uri)
                                .key(authKey)
                                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                                .gatewayMode()
                                .contentResponseOnWriteEnabled(true)
                                .clientTelemetryEnabled(false)
                                .buildClient()
                        }
                    }
                    runBlocking {
                        withTimeout(Duration.ofSeconds(10).toMillis()) {
                            client = d?.await()
                        } // wait with timeout
                    }
                    client
                } catch (ex: TimeoutCancellationException) {
                    null
                }
            }
            return client
        }

}