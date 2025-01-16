package gov.cdc.ocio.processingstatusapi.health.messagesystem

import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the unsupported messaging service health checks.
 */
class HealthCheckUnsupportedMessageSystem(
    private val messageSystem: String
) : HealthCheckSystem("Messaging Service") {

    /**
     * No health check - just inform unsupported
     */
    override fun doHealthCheck(): HealthCheckResult {
        return HealthCheckResult(
            service,
            HealthStatusType.STATUS_DOWN,
            healthIssues = "Unsupported message system: $messageSystem"
        )
    }
}