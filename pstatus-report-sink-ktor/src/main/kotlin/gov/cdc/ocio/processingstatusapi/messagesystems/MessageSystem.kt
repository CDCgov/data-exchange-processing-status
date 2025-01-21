package gov.cdc.ocio.processingstatusapi.messagesystems

import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * Defines the interface for a Message System.
 *
 * @property healthCheckSystem HealthCheckSystem
 */
interface MessageSystem {
    var healthCheckSystem: HealthCheckSystem
}