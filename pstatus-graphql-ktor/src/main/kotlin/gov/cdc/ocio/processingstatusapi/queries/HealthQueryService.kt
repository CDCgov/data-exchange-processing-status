package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.loaders.ReportLoader
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import kotlin.system.measureTimeMillis

@Serializable
abstract class HealthCheckSystem {

    @GraphQLDescription("Status of the dependency")
    var status: String = "DOWN"

    @GraphQLDescription("Service health issues")
    var healthIssues: String? = ""

    @GraphQLDescription("Name of the service")
    open val service: String = ""
}

class CosmosDb: HealthCheckSystem() {
    override val service = "Cosmos DB"
}

@Serializable
class HealthCheck {

    @GraphQLDescription("Overall status of the service")
    var status : String? = "DOWN"

    @GraphQLDescription("Total time it took to evaluate the health of the service and its dependencies")
    var totalChecksDuration : String? = null

    @GraphQLDescription("Status of the service dependencies")
    var dependencyHealthChecks = mutableListOf<HealthCheckSystem>()
}

class HealthQueryService : Query {

    private val logger = KotlinLogging.logger {}

    @GraphQLDescription("Performs a service health check of the processing status API and it's dependencies.")
    @Suppress("unused")
    fun getHealth(): HealthCheck {
        var cosmosDBHealthy = false
        val cosmosDBHealth = CosmosDb()
        val time = measureTimeMillis {
            try {
                cosmosDBHealthy = isCosmosDBHealthy()
                cosmosDBHealth.status = "UP"
            } catch (ex: Exception) {
                cosmosDBHealth.healthIssues = ex.message
                logger.error("CosmosDB is not healthy: ${ex.message}")
            }
        }

        return HealthCheck().apply {
            status = if (cosmosDBHealthy) "UP" else "DOWN"
            totalChecksDuration = formatMillisToHMS(time)
            dependencyHealthChecks.add(cosmosDBHealth)
        }
    }

    /**
     * Check whether CosmosDB is healthy.
     *
     * @return Boolean
     */
    private fun isCosmosDBHealthy(): Boolean {
        return ReportLoader().getAnyReport() != null
    }

    /**
     * Format the time in milliseconds to 00:00:00.000 format.
     *
     * @param millis Long
     * @return String
     */
    private fun formatMillisToHMS(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        val remainingMillis = millis % 1000

        return "%02d:%02d:%02d.%03d".format(hours, minutes, remainingSeconds, remainingMillis / 10)
    }
}