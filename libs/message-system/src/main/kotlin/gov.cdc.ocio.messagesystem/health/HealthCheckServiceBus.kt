package gov.cdc.ocio.messagesystem.health

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder
import com.azure.messaging.servicebus.administration.models.EntityStatus
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.messagesystem.configs.AzureServiceBusConfiguration
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent


/**
 * Concrete implementation of the ASB messaging service health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckServiceBus(
    system: String,
    private val azureServiceBusConfiguration: AzureServiceBusConfiguration
) : HealthCheckSystem(system, "Azure Service Bus"), KoinComponent {

    /**
     * Checks and sets azureServiceBusHealth status
     *
     * @return HealthCheckServiceBus
     */
    override fun doHealthCheck(): HealthCheckResult {

        val result = isServiceBusHealthy(azureServiceBusConfiguration)
        result.onFailure { error ->
            val reason = "Azure Service Bus is not healthy: ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(system, service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(system, service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether service bus is healthy.
     *
     * @param config AzureServiceBusConfiguration
     * @return Result<Boolean>
     */
    private fun isServiceBusHealthy(config: AzureServiceBusConfiguration): Result<Boolean> {
        return try {
            val adminClient = ServiceBusAdministrationClientBuilder()
                .connectionString(config.connectionString)
                .buildClient()

            // Get the properties of the topic to check the connection
            val result = adminClient.getTopic(config.topicName)
            if (result.status == EntityStatus.ACTIVE)
                Result.success(true)
            Result.failure(Exception("Failed to get the status of the topic"))
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }
}