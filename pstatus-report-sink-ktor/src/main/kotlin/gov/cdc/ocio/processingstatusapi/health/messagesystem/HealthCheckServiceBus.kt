package gov.cdc.ocio.processingstatusapi.health.messagesystem

import com.azure.core.exception.ResourceNotFoundException
import com.azure.messaging.servicebus.ServiceBusException
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.processingstatusapi.health.HealthCheck
import gov.cdc.ocio.processingstatusapi.health.HealthCheckSystem
import gov.cdc.ocio.processingstatusapi.plugins.AzureServiceBusConfiguration
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Concrete implementation of the ASB messaging service health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckServiceBus : HealthCheckSystem("Azure Service Bus"), KoinComponent {

    private val azureServiceBusConfiguration by inject<AzureServiceBusConfiguration>()

    /**
     * Checks and sets azureServiceBusHealth status
     *
     * @return HealthCheckServiceBus
     */
    override fun doHealthCheck() {
        try {
            if (isServiceBusHealthy(azureServiceBusConfiguration)) {
                status = HealthCheck.STATUS_UP
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
    private fun isServiceBusHealthy(config: AzureServiceBusConfiguration): Boolean {

        val adminClient = ServiceBusAdministrationClientBuilder()
            .connectionString(config.connectionString)
            .buildClient()

        // Get the properties of the topic to check the connection
        adminClient.getTopic(config.topicName)

        return true
    }
}