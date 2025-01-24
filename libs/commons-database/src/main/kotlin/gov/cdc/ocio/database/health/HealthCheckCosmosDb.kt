package gov.cdc.ocio.database.health

import com.azure.cosmos.CosmosClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent


/**
 * Concrete implementation of the cosmosdb health check.
 */
@JsonIgnoreProperties("koin")
class HealthCheckCosmosDb(
    system: String,
    private val cosmosClient: CosmosClient
) : HealthCheckSystem(system, "Cosmos DB"), KoinComponent {

    /**
     * Checks and sets cosmosDBHealth status
     *
     * @return HealthCheckResult
     */
    override fun doHealthCheck(): HealthCheckResult {
        val databaseResponse = cosmosClient.getDatabase("ProcessingStatus")
        return if (databaseResponse != null)
            HealthCheckResult(
                system,
                service,
                HealthStatusType.STATUS_DOWN,
                healthIssues = "Cosmos DB is not healthy: Failed to establish a CosmosDB client.")
        else
            HealthCheckResult(system, service, HealthStatusType.STATUS_UP)
    }
}