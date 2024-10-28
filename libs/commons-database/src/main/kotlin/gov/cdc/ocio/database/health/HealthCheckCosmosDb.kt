package gov.cdc.ocio.database.health

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.database.cosmos.CosmosClientManager
import gov.cdc.ocio.database.cosmos.CosmosConfiguration
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Concrete implementation of the cosmosdb health check.
 */
@JsonIgnoreProperties("koin")
class HealthCheckCosmosDb : HealthCheckSystem("Cosmos DB"), KoinComponent {

    private val cosmosConfiguration by inject<CosmosConfiguration>()

    /**
     * Checks and sets cosmosDBHealth status
     */
    override fun doHealthCheck() {
        try {
            if (isCosmosDBHealthy()) {
                status = HealthStatusType.STATUS_UP
            }
        } catch (ex: Exception) {
            logger.error("Cosmos DB is not healthy $ex.message")
            healthIssues = ex.message
        }
    }

    /**
     * Check whether CosmosDB is healthy.
     *
     * @return Boolean
     */
    private fun isCosmosDBHealthy(): Boolean {
        return if (CosmosClientManager.getCosmosClient(cosmosConfiguration.uri, cosmosConfiguration.authKey) == null)
            throw Exception("Failed to establish a CosmosDB client.")
        else
            true
    }
}