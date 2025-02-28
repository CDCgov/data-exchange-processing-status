package gov.cdc.ocio.messagesystem.rabbitmq

import com.rabbitmq.client.Connection
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

    val rabbitMQConnection: Connection? by lazy {
        try {
            rabbitMQConfig.getConnectionFactory().newConnection()
        } catch (e: Exception) {
            null
        }
    }

    override var healthCheckSystem = HealthCheckRabbitMQ(system, rabbitMQConfig) as HealthCheckSystem
}