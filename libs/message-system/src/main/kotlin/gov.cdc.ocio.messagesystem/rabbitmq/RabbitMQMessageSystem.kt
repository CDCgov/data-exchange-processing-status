package gov.cdc.ocio.messagesystem.rabbitmq

import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.health.HealthCheckRabbitMQ
import gov.cdc.ocio.messagesystem.config.RabbitMQServiceConfiguration
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * Implementation of the RabbitMQ message system.
 *
 * @property rabbitMQConnection Connection
 * @property healthCheckSystem HealthCheckSystem
 * @constructor
 */
class RabbitMQMessageSystem(
    rabbitMQConfig: RabbitMQServiceConfiguration
): MessageSystem {

    val rabbitMQConnection by lazy {
        try {
            rabbitMQConfig.getConnectionFactory().newConnection()
        } catch (e: Exception) {
            null
        }
    }

    // Create send channel
    private val sendChannel by lazy {
        rabbitMQConnection?.createChannel()?.apply {
            queueDeclare(
                rabbitMQConfig.sendQueueName,
                true,
                false,
                false,
                null
            )
        }
    }

    private val sendQueueName = rabbitMQConfig.sendQueueName

    override var healthCheckSystem = HealthCheckRabbitMQ(system, rabbitMQConfig) as HealthCheckSystem

    /**
     * Sends a message to a queue.
     *
     * @param message String
     */
    override fun send(message: String) {
        sendChannel?.basicPublish(
            "", // use default exchange
            sendQueueName, // routing key must equal queue name
            null,
            message.toByteArray(Charsets.UTF_8)
        )
    }
}