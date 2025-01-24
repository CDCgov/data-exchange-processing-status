package gov.cdc.ocio.processingstatusnotifications

import com.azure.core.exception.ResourceNotFoundException
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder
import com.microsoft.azure.servicebus.primitives.ServiceBusException
import gov.cdc.ocio.processingstatusnotifications.servicebus.AzureServiceBusConfiguration
import gov.cdc.ocio.types.utils.TimeUtils
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
 * @property azureServiceBusConfiguration AzureServiceBusConfiguration
 */
class HealthQueryService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val azureServiceBusConfiguration by inject<AzureServiceBusConfiguration>()

    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        var serviceBusHealthy = false
         val serviceBusHealth = HealthCheckServiceBus()
        val time = measureTimeMillis {
            try {
                serviceBusHealthy = isServiceBusHealthy(config = azureServiceBusConfiguration)
                serviceBusHealth.status = "UP"
            } catch (ex: Exception) {
                serviceBusHealth.healthIssues = ex.message
                logger.error("Azure Service Bus is not healthy: ${ex.message}")
            }
        }

        return HealthCheck().apply {
            status = if (serviceBusHealthy) "UP" else "DOWN"
            totalChecksDuration = TimeUtils.formatMillisToHMS(time)
            dependencyHealthChecks.add(serviceBusHealth)
        }
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
}