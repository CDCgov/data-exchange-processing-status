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
    val connectionString = config.tryGetString("${configPath}connection_string") ?: ""
    val queueName = config.tryGetString("${configPath}queue_name") ?: ""
    val topicName = config.tryGetString("${configPath}topic_name") ?: ""
    val subscriptionName = config.tryGetString("${configPath}subscription_name") ?: ""
}
