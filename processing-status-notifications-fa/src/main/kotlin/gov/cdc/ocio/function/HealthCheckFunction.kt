package gov.cdc.ocio.function

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.servicebus.QueueClient
import com.microsoft.azure.servicebus.ReceiveMode
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder
import com.microsoft.azure.servicebus.primitives.ServiceBusException
import gov.cdc.ocio.model.HealthCheck
import gov.cdc.ocio.model.ServiceBus
import mu.KotlinLogging
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Run health checks for the service.
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property logger (Logger..Logger?)
 * @constructor
 */
class HealthCheckFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {
    val logger = KotlinLogging.logger {}

    fun run(): HttpResponseMessage {
        var serviceBusHealthy = false
        val serviceBusHealth = ServiceBus()
        val time = measureTimeMillis {
            try {
                serviceBusHealthy = isServiceBusHealthy()
                serviceBusHealth.status = "UP"
            } catch (ex: Exception) {
                serviceBusHealth.healthIssues = ex.message
                logger.error("Azure Service Bus is not healthy: ${ex.message}")
            }
        }

        val result = HealthCheck().apply {
            status = if (serviceBusHealthy) "UP" else "DOWN"
            totalChecksDuration = formatMillisToHMS(time)
            dependencyHealthChecks.add(serviceBusHealth)
        }

        if(result.status == "DOWN"){
            return request
                .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "application/json")
                .body(result)
                .build()
        }

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

    /**
     * Check whether service bus is healthy.
     *
     * @return Boolean
     */
    @Throws(InterruptedException::class, ServiceBusException::class)
    private fun isServiceBusHealthy(): Boolean {
        val connectionString = System.getenv("ServiceBusConnectionString")
        val queueName = System.getenv("ServiceBusQueueName")
        val queueClient = QueueClient(ConnectionStringBuilder(connectionString, queueName), ReceiveMode.PEEKLOCK)
        queueClient.close()
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