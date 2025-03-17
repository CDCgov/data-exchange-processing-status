package gov.cdc.ocio.messagesystem.config

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
    val listenQueueName = config.tryGetString("${configPath}service_bus.listen_queue_name") ?: ""
    val listenTopicName = config.tryGetString("${configPath}service_bus.listen_topic_name") ?: ""
    val sendQueueName = config.tryGetString("${configPath}service_bus.send_queue_name") ?: ""
    val subscriptionName = config.tryGetString("${configPath}service_bus.subscription_name") ?: ""
}
