package gov.cdc.ocio.processingstatusapi.messagesystems

import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckServiceBus
import gov.cdc.ocio.processingstatusapi.plugins.AzureServiceBusConfiguration
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

    override var healthCheckSystem = HealthCheckServiceBus(config) as HealthCheckSystem
}