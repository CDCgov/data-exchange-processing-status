package gov.cdc.ocio.messagesystem.unsupported

import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.health.HealthCheckUnsupportedMessageSystem
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * Implementation of the unsupported message system.
 *
 * @property healthCheckSystem HealthCheckSystem
 * @constructor
 */
class UnsupportedMessageSystem(messageSystem: String?): MessageSystem {

    override var healthCheckSystem = HealthCheckUnsupportedMessageSystem(system, messageSystem) as HealthCheckSystem

    override fun send(message: String) {
        throw NotImplementedError()
    }
}