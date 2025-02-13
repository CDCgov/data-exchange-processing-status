package gov.cdc.ocio.messagesystem.plugins

import com.rabbitmq.client.*
import io.ktor.server.config.*

/**
 * The `RabbitMQServiceConfiguration` class configures and initializes `RabbitMQ` connection factory based on settings
 * provided in an `ApplicationConfig`.
 *
 * @param config `ApplicationConfig` containing the configuration settings for RabbitMQ, including connection details.
 * @param configurationPath represents prefix used to locate environment variables specific to RabbitMQ within the
 * configuration.
 */
class RabbitMQServiceConfiguration(
    config: ApplicationConfig,
    configurationPath: String? = null
) {
    companion object {
        const val DEFAULT_HOST = "localhost"
        const val DEFAULT_VIRTUAL_HOST = "/"
        const val DEFAULT_USERNAME = "guest"
        const val DEFAULT_PASSWORD = "guest"
    }

    private val connectionFactory: ConnectionFactory = ConnectionFactory()
    private val configPath= if (configurationPath != null) "$configurationPath." else ""
    val queue = config.tryGetString("${configPath}queue_name") ?: ""

    init {
        connectionFactory.host = config.tryGetString("${configPath}host") ?: DEFAULT_HOST
        connectionFactory.port = config.tryGetString("${configPath}port") ?.toInt()!!
        connectionFactory.virtualHost = config.tryGetString("${configPath}virtual_host") ?: DEFAULT_VIRTUAL_HOST
        connectionFactory.username = config.tryGetString("${configPath}user_name") ?: DEFAULT_USERNAME
        connectionFactory.password = config.tryGetString("${configPath}password") ?: DEFAULT_PASSWORD

        // Attempt recovery every 10 seconds
        connectionFactory.setNetworkRecoveryInterval(10000)
        connectionFactory.connectionTimeout = 10000 // 10 seconds
    }

    fun getConnectionFactory(): ConnectionFactory {
        return connectionFactory
    }
}
