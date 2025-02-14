package gov.cdc.ocio.messagesystem.health

import com.rabbitmq.client.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.rabbitmq.client.Channel
import gov.cdc.ocio.messagesystem.configs.RabbitMQServiceConfiguration
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import java.net.UnknownHostException
import java.time.Duration
import java.util.concurrent.TimeoutException


/**
 * Concrete implementation of the RabbitMQ messaging service health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckRabbitMQ(
    system: String,
    private val rabbitMQConfig: RabbitMQServiceConfiguration
) : HealthCheckSystem(system, "RabbitMQ"), KoinComponent {

    /**
     * Checks and sets rabbitMQHealth status
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = runBlocking {
            async {
                isRabbitMQHealthy()
            }.await()
        }
        result.onFailure { error ->
            val reason = "RabbitMQ is not healthy: ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(system, service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(system, service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether rabbitMQ is healthy.
     *
     * @return Result<Boolean>
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun isRabbitMQHealthy(): Result<Boolean> {
        var channel: Channel? = null
        var rabbitMQConnection: Connection? = null
        val issue: String
        try {
            rabbitMQConnection = rabbitMQConfig.getConnectionFactory().newConnection()

            var d: Deferred<Channel?>? = null
            GlobalScope.launch {
                d = async {
                    rabbitMQConnection?.createChannel()
                }
            }
            runBlocking {
                withTimeout(Duration.ofSeconds(5).toMillis()) {
                    channel = d?.await()
                } // wait with timeout
            }
            val isOpen = channel?.isOpen ?: false
            channel?.close()
            rabbitMQConnection?.close()
            return if (!isOpen)
                Result.failure(Exception("Established RabbitMQ connection, but failed to create a channel."))
            else
                Result.success(true)
        } catch (ex: UnknownHostException) {
            issue = "Unknown host: ${ex.localizedMessage}"
        } catch (ex: TimeoutException) {
            issue = "Timeout"
        } catch (ex: Exception) {
            issue = ex.localizedMessage
        }

        channel?.close()
        rabbitMQConnection?.close()

        return Result.failure(Exception("Failed to establish connection to RabbitMQ server: $issue"))
    }
}