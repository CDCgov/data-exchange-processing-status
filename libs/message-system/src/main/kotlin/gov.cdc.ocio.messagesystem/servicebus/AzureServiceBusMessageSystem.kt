package gov.cdc.ocio.messagesystem.servicebus

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusMessage
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.config.AzureServiceBusConfiguration
import gov.cdc.ocio.messagesystem.health.HealthCheckServiceBus
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * Implementation of the Azure Service Bus message system.
 *
 * @property healthCheckSystem HealthCheckSystem
 * @constructor
 */
class AzureServiceBusMessageSystem(
    config: AzureServiceBusConfiguration
): MessageSystem {

    override var healthCheckSystem = HealthCheckServiceBus(system, config) as HealthCheckSystem

    private val senderClient = ServiceBusClientBuilder()
        .connectionString(config.connectionString)
        .sender()
        .queueName(config.sendQueueName)
        .buildClient()

    override fun send(message: String) = senderClient.sendMessage(ServiceBusMessage(message))
}