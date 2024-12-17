package gov.cdc.ocio.processingstatusapi.health.messagesystem

import com.azure.core.exception.ResourceNotFoundException
import com.azure.messaging.servicebus.ServiceBusException
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.processingstatusapi.plugins.AzureConfiguration
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Concrete implementation of the ASB messaging service health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckServiceBus : HealthCheckSystem("Azure Service Bus"), KoinComponent {

    private val azureServiceBusConfiguration by inject<AzureConfiguration>()

    /**
     * Checks and sets azureServiceBusHealth status
     *
     * @return HealthCheckServiceBus
     */
    override fun doHealthCheck() {
        try {
            if (isServiceBusHealthy(azureServiceBusConfiguration)) {
                status = HealthStatusType.STATUS_UP
            }
        } catch (ex: Exception) {
            logger.error("Azure Service Bus is not healthy $ex.message")
            healthIssues = ex.message
        }
    }

    /**
     * Check whether service bus is healthy.
     *
     * @return Boolean
     */
    @Throws(ResourceNotFoundException::class, ServiceBusException::class)
    private fun isServiceBusHealthy(config: AzureConfiguration): Boolean {

        val adminClient = ServiceBusAdministrationClientBuilder()
            .connectionString(config.connectionString)
            .buildClient()

        // Get the properties of the topic to check the connection
        adminClient.getTopic(config.topicName)

        return true
    }
}