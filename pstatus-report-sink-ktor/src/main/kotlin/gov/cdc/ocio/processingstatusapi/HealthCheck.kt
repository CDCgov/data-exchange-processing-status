package gov.cdc.ocio.processingstatusapi

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode
import com.microsoft.azure.servicebus.primitives.ServiceBusException
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.models.Report
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
 * @property cosmosRepository CosmosRepository
 * @property azureServiceBusConfiguration AzureServiceBusConfiguration
 */
class HealthQueryService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val cosmosRepository by inject<CosmosRepository>()

    private val azureServiceBusConfiguration by inject<AzureServiceBusConfiguration>()

    fun getHealth(): HealthCheck {
        var cosmosDBHealthy = false
        var serviceBusHealthy = false
        val cosmosDBHealth = HealthCheckCosmosDb()
        val serviceBusHealth = HealthCheckServiceBus()
        val time = measureTimeMillis {
            try {
                cosmosDBHealthy = isCosmosDBHealthy()
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
     * @return Boolean
     */
    private fun isCosmosDBHealthy(): Boolean {
        val sqlQuery = "select * t offset 0 limit 1"
        cosmosRepository.reportsContainer?.queryItems(
            sqlQuery, CosmosQueryRequestOptions(),
            Report::class.java
        )
        return true
    }

    /**
     * Check whether service bus is healthy.
     *
     * @return Boolean
     */
    @Throws(InterruptedException::class, ServiceBusException::class)
    private fun isServiceBusHealthy(config: AzureServiceBusConfiguration): Boolean {

        val receiverClient = ServiceBusClientBuilder()
            .connectionString(config.connectionString)
            .receiver()
            .topicName(config.topicName)
            .subscriptionName(config.subscriptionName)
            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK) // PEEK_LOCK mode to avoid consuming messages
            .buildClient()

        // Attempt to open the connection
        receiverClient.peekMessage()

        // Close the receiver client
        receiverClient.close()

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