package gov.cdc.ocio.processingstatusapi.messagesystems

import com.rabbitmq.client.Connection
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckRabbitMQ
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * Implementation of the RabbitMQ message system.
 *
 * @property rabbitMQConnection Connection
 * @property healthCheckSystem HealthCheckSystem
 * @constructor
 */
class RabbitMQMessageSystem(val rabbitMQConnection: Connection): MessageSystem {

    override var healthCheckSystem = HealthCheckRabbitMQ(rabbitMQConnection) as HealthCheckSystem
}