package gov.cdc.ocio.messagesystem.config

import gov.cdc.ocio.messagesystem.MessageProcessorInterface
import io.ktor.server.config.*


/**
 * Azure Service Bus configuration values
 *
 * @property configPath String
 * @property connectionString String
 * @property listenQueueName String
 * @property listenTopicName String
 * @property sendQueueName String
 * @property subscriptionName String
 * @constructor
 */
class AzureServiceBusConfiguration(
    config: ApplicationConfig,
    configurationPath: String? = null
) {
    lateinit var messageProcessor: MessageProcessorInterface

    private val configPath = if (configurationPath != null) "$configurationPath." else ""

    val connectionString = config.tryGetString("${configPath}connection_string") ?: ""
    val listenQueueName = config.tryGetString("${configPath}listen_queue_name") ?: ""
    val listenTopicName = config.tryGetString("${configPath}listen_topic_name") ?: ""
    val sendQueueName = config.tryGetString("${configPath}send_queue_name") ?: ""
    val subscriptionName = config.tryGetString("${configPath}subscription_name") ?: ""
}
