package gov.cdc.ocio.processingstatusapi.messagesystems

import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckUnsupportedMessageSystem
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * Implementation of the unsupported message system.
 *
 * @property healthCheckSystem HealthCheckSystem
 * @constructor
 */
class UnsupportedMessageSystem(messageSystem: String?): MessageSystem {

    override var healthCheckSystem = HealthCheckUnsupportedMessageSystem(system, messageSystem) as HealthCheckSystem
}