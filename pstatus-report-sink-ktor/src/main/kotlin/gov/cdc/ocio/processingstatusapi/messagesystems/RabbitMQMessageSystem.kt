package gov.cdc.ocio.processingstatusapi.messagesystems

import com.rabbitmq.client.Connection
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckRabbitMQ
import gov.cdc.ocio.messagesystem.plugins.RabbitMQServiceConfiguration
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