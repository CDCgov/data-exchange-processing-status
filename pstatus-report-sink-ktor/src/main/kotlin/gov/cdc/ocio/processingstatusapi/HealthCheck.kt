package gov.cdc.ocio.processingstatusapi

import com.azure.core.exception.ResourceNotFoundException
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder
import com.microsoft.azure.servicebus.primitives.ServiceBusException
import com.rabbitmq.client.Connection
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosClientManager
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosConfiguration
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.plugins.AzureServiceBusConfiguration
import gov.cdc.ocio.processingstatusapi.plugins.RabbitMQServiceConfiguration
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
 * Concrete implementation of the RabbitMQ health check.
 *
 * @property service String
 */
class HealthCheckRabbitMQ: HealthCheckSystem() {
    override val service: String = "RabbitMQ"
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
 * @property rabbitMQServiceConfiguration RabbitMQServiceConfiguration
 */
class HealthQueryService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val cosmosConfiguration by inject<CosmosConfiguration>()

    private val azureServiceBusConfiguration by inject<AzureServiceBusConfiguration>()

    private val rabbitMQServiceConfiguration by inject<RabbitMQServiceConfiguration>()

    private val msgType: String by inject()


    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        var cosmosDBHealthy = false
        var serviceBusHealthy = false
        var rabbitMQHealthy = false
        val cosmosDBHealth = HealthCheckCosmosDb()
        lateinit var rabbitMQHealth: HealthCheckRabbitMQ
        lateinit var serviceBusHealth: HealthCheckServiceBus


        val time = measureTimeMillis {
            try {
                cosmosDBHealthy = isCosmosDBHealthy(config = cosmosConfiguration)
                cosmosDBHealth.status = "UP"
            } catch (ex: Exception) {
                cosmosDBHealth.healthIssues = ex.message
                logger.error("CosmosDB is not healthy: ${ex.message}")
            }
            // selectively check health of the messaging service based on msgType
            when (msgType) {
                MessageSystem.AZURE_SERVICE_BUS.toString() -> {
                    serviceBusHealth = HealthCheckServiceBus()
                    try {
                        serviceBusHealthy = isServiceBusHealthy(config = azureServiceBusConfiguration)
                        serviceBusHealth.status = "UP"
                    } catch (ex: Exception) {
                        serviceBusHealth.healthIssues = ex.message
                        logger.error("Azure Service Bus is not healthy: ${ex.message}")
                    }
                }
                MessageSystem.RABBITMQ.toString() -> {
                    rabbitMQHealth = HealthCheckRabbitMQ()
                    try {
                        rabbitMQHealthy = isRabbitMQHealthy(config = rabbitMQServiceConfiguration)
                        rabbitMQHealth.status = "UP"
                    } catch (ex: Exception) {
                        rabbitMQHealth.healthIssues = ex.message
                        logger.error("RabbitMQ is not healthy: ${ex.message}")
                    }
                }
            }
        }
        return HealthCheck().apply {
            status = if (cosmosDBHealthy && (serviceBusHealthy || rabbitMQHealthy)) "UP" else "DOWN"
            totalChecksDuration = formatMillisToHMS(time)
            dependencyHealthChecks.add(cosmosDBHealth)
            when (msgType) {
                MessageSystem.AZURE_SERVICE_BUS.toString() -> {
                    dependencyHealthChecks.add(serviceBusHealth)
                }
                MessageSystem.RABBITMQ.toString() -> {
                    dependencyHealthChecks.add(rabbitMQHealth)
                }
            }

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
     * Check whether rabbitMQ is healthy.
     *
     * @return Boolean
     */
    @Throws(BadStateException::class)
    private fun isRabbitMQHealthy(config: RabbitMQServiceConfiguration): Boolean {
        var rabbitMQConnection: Connection? = null
        return try {
            rabbitMQConnection = config.getConnectionFactory().newConnection()
            rabbitMQConnection.isOpen
        }catch (e: Exception) {
            throw Exception("Failed to establish connection to RabbitMQ server.")
        } finally {
            rabbitMQConnection?.close()
        }
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