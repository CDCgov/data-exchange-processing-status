package gov.cdc.ocio.processingstatusapi

import com.azure.core.exception.ResourceNotFoundException
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder
import com.microsoft.azure.servicebus.primitives.ServiceBusException
import gov.cdc.ocio.database.cosmos.CosmosClientManager
import gov.cdc.ocio.database.cosmos.CosmosConfiguration
import gov.cdc.ocio.processingstatusapi.plugins.AzureServiceBusConfiguration
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis


/**
 * Abstract class used for modeling the health issues of an individual service.
 *
 * @property status String
 * @property healthIssues String?
 * @property service String
 */
abstract class HealthCheckSystem {

    var status: String = "DOWN"

    var healthIssues: String? = ""

    open val service: String = ""
}

/**
 * Concrete implementation of the cosmosdb service health check.
 *
 * @property service String
 */
class HealthCheckCosmosDb: HealthCheckSystem() {
    override val service = "Cosmos DB"
}

/**
 * Concrete implementation of the Azure Service Bus health check.
 *
 * @property service String
 */
class HealthCheckServiceBus: HealthCheckSystem() {
    override val service: String = "Azure Service Bus"
}

/**
 * Run health checks for the service.
 *
 * @property status String?
 * @property totalChecksDuration String?
 * @property dependencyHealthChecks MutableList<HealthCheckSystem>
 */
class HealthCheck {

    var status: String = "DOWN"

    var totalChecksDuration: String? = null

    var dependencyHealthChecks = mutableListOf<HealthCheckSystem>()
}

/**
 * Service for querying the health of the report-sink service and its dependencies.
 *
 * @property logger KLogger
 * @property cosmosConfiguration CosmosConfiguration
 * @property azureServiceBusConfiguration AzureServiceBusConfiguration
 */
class HealthQueryService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val cosmosConfiguration by inject<CosmosConfiguration>()

    private val azureServiceBusConfiguration by inject<AzureServiceBusConfiguration>()

    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        var cosmosDBHealthy = false
        var serviceBusHealthy = false
        val cosmosDBHealth = HealthCheckCosmosDb()
        val serviceBusHealth = HealthCheckServiceBus()
        val time = measureTimeMillis {
            try {
                cosmosDBHealthy = isCosmosDBHealthy(config = cosmosConfiguration)
                cosmosDBHealth.status = "UP"
            } catch (ex: Exception) {
                cosmosDBHealth.healthIssues = ex.message
                logger.error("CosmosDB is not healthy: ${ex.message}")
            }

            try {
                serviceBusHealthy = isServiceBusHealthy(config = azureServiceBusConfiguration)
                serviceBusHealth.status = "UP"
            } catch (ex: Exception) {
                serviceBusHealth.healthIssues = ex.message
                logger.error("Azure Service Bus is not healthy: ${ex.message}")
            }
        }

        return HealthCheck().apply {
            status = if (cosmosDBHealthy && serviceBusHealthy) "UP" else "DOWN"
            totalChecksDuration = formatMillisToHMS(time)
            dependencyHealthChecks.add(cosmosDBHealth)
            dependencyHealthChecks.add(serviceBusHealth)
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

    /**
     * Check whether service bus is healthy.
     *
     * @return Boolean
     */
    @Throws(ResourceNotFoundException::class, ServiceBusException::class)
    private fun isServiceBusHealthy(config: AzureServiceBusConfiguration): Boolean {

        val adminClient = ServiceBusAdministrationClientBuilder()
            .connectionString(config.connectionString)
            .buildClient()

        // Get the properties of the topic to check the connection
        adminClient.getTopic(config.topicName)

        return true
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