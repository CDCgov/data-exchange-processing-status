package gov.cdc.ocio.processingstatusnotifications

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder
import com.azure.messaging.servicebus.administration.models.EntityStatus
import gov.cdc.ocio.messagesystem.config.AzureServiceBusConfiguration
import gov.cdc.ocio.types.health.HealthCheck
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthStatusType
import gov.cdc.ocio.types.utils.TimeUtils
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis


/**
 * Service for querying the health of the report-sink service and its dependencies.
 *
 * @property logger KLogger
 * @property azureServiceBusConfiguration AzureServiceBusConfiguration
 */
class HealthQueryService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val system = "Message System"

    private val service = "Azure Service Bus"

    private val azureServiceBusConfiguration by inject<AzureServiceBusConfiguration>()

    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        val messageSystemHealthCheck: HealthCheckResult

        val time = measureTimeMillis {
            messageSystemHealthCheck = checkHealthMessageSystem()
        }

        return HealthCheck().apply {
            status = messageSystemHealthCheck.status
            totalChecksDuration = TimeUtils.formatMillisToHMS(time)
            dependencyHealthChecks.add(messageSystemHealthCheck)
        }
    }

    /**
     * Checks the health of the message system.
     *
     * @return HealthCheckResult
     */
    private fun checkHealthMessageSystem(): HealthCheckResult {
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