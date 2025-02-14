package gov.cdc.ocio.messagesystem.configs

import io.ktor.server.config.*

/**
 * Class which initializes configuration values
 *
 * @property configPath String
 * @property connectionString String
 * @property queueName String
 * @property topicName String
 * @property subscriptionName String
 * @constructor
 */
class AzureServiceBusConfiguration(
    config: ApplicationConfig,
    configurationPath: String? = null
) {

    private val configPath = if (configurationPath != null) "$configurationPath." else ""
    val connectionString = config.tryGetString("${configPath}service_bus.connection_string") ?: ""
    val queueName = config.tryGetString("${configPath}service_bus.queue_name") ?: ""
    val topicName = config.tryGetString("${configPath}service_bus.topic_name") ?: ""
    val subscriptionName = config.tryGetString("${configPath}service_bus.subscription_name") ?: ""
}
