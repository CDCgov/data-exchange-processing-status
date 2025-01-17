package gov.cdc.ocio.processingstatusapi.health.messagesystem

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import java.time.Duration


/**
 * Concrete implementation of the RabbitMQ messaging service health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckRabbitMQ(private val rabbitMQConnection: Connection) : HealthCheckSystem("RabbitMQ"), KoinComponent {

    /**
     * Checks and sets rabbitMQHealth status
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = runBlocking {
            async {
                isRabbitMQHealthy1()
            }.await()
        }
        result.onFailure { error ->
            val reason ="RabbitMQ is not healthy ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether rabbitMQ is healthy.
     *
     * @return Boolean
     */
    @Throws(Exception::class)
    private suspend fun isRabbitMQHealthy(): Result<Boolean> {
        var channel: Channel? = null
        try {
            if (rabbitMQConnection.isOpen) {
                channel = withTimeoutOrNull(2000L) {
                    rabbitMQConnection.createChannel()
                }
                val isOpen = channel?.isOpen ?: false
                channel?.close()
                if (!isOpen)
                    return Result.failure(Exception("Established RabbitMQ connection, but failed to create a channel."))
            }
        } catch (e: Exception) {
            channel?.close()
            return Result.failure(Exception("Failed to establish connection to RabbitMQ server: ${e.localizedMessage}"))
        }
        return Result.success(true)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun isRabbitMQHealthy1(): Result<Boolean> {
        var channel: Channel? = null
        try {
            var d: Deferred<Channel>? = null
            GlobalScope.launch {
                d = async {
                    rabbitMQConnection.createChannel()
                }
            }
            runBlocking {
                withTimeout(Duration.ofSeconds(5).toMillis()) {
                    channel = d?.await()
                } // wait with timeout
            }
            val isOpen = channel?.isOpen ?: false
            channel?.close()
            if (!isOpen)
                return Result.failure(Exception("Established RabbitMQ connection, but failed to create a channel."))
        } catch (ex: TimeoutCancellationException) {
            channel?.close()
            return Result.failure(Exception("Failed to establish connection to RabbitMQ server: ${ex.localizedMessage}"))
        }
        return Result.success(true)
    }
}