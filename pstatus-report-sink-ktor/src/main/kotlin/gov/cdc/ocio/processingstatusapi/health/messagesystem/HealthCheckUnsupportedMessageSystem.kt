package gov.cdc.ocio.processingstatusapi.health.messagesystem

import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the unsupported messaging service health checks.
 */
class HealthCheckUnsupportedMessageSystem : HealthCheckSystem("Messaging Service") {

    /**
     * No health check - just inform unsupported
     */
    override fun doHealthCheck() {
        status = HealthStatusType.STATUS_UNSUPPORTED
    }
}