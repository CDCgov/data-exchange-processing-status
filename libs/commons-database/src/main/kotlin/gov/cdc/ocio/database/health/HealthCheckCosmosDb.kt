package gov.cdc.ocio.database.health

import com.azure.cosmos.CosmosClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent

/**
 * Concrete implementation of the cosmosdb health check.
 */
@JsonIgnoreProperties("koin")
class HealthCheckCosmosDb(private val cosmosClient: CosmosClient) : HealthCheckSystem("Cosmos DB"), KoinComponent {

    /**
     * Checks and sets cosmosDBHealth status
     */
    override fun doHealthCheck() {
        try {
            if (isCosmosDbHealthy()) {
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
    private fun isCosmosDbHealthy(): Boolean {
        val databaseResponse = cosmosClient.getDatabase("ProcessingStatus")
        return if (databaseResponse == null)
            throw Exception("Failed to establish a CosmosDB client.")
        else
            true
    }
}