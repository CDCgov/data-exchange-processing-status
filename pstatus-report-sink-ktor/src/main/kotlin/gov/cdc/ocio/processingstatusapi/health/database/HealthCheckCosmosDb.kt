package gov.cdc.ocio.processingstatusapi.health.database

import gov.cdc.ocio.database.cosmos.CosmosClientManager
import gov.cdc.ocio.database.cosmos.CosmosConfiguration
import gov.cdc.ocio.processingstatusapi.health.HealthCheck
import gov.cdc.ocio.processingstatusapi.health.HealthCheckSystem
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Concrete implementation of the cosmosdb health check.
 */
class HealthCheckCosmosDb : HealthCheckSystem("Cosmos DB"), KoinComponent {

    private val cosmosConfiguration by inject<CosmosConfiguration>()

    /**
     * Checks and sets cosmosDBHealth status
     */
    override fun doHealthCheck() {
        try {
            if (isCosmosDBHealthy(cosmosConfiguration)) {
                status = HealthCheck.STATUS_UP
            }
        } catch (ex: Exception) {
            logger.error("Cosmos DB is not healthy $ex.message")
        }
    }

    /**
     * Check whether CosmosDB is healthy.
     *
     * @param config CosmosConfiguration
     * @return Boolean
     */
    private fun isCosmosDBHealthy(config: CosmosConfiguration): Boolean {
        return if (CosmosClientManager.getCosmosClient(config.uri, config.authKey) == null)
            throw Exception("Failed to establish a CosmosDB client.")
        else
            true
    }
}