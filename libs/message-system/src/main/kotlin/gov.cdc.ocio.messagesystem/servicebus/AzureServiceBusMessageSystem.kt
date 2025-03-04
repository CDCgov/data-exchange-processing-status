package gov.cdc.ocio.messagesystem.servicebus

import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.config.AzureServiceBusConfiguration
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.messagesystem.health.HealthCheckServiceBus


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
}