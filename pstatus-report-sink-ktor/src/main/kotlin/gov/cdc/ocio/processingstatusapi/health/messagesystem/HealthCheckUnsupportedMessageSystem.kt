package gov.cdc.ocio.processingstatusapi.health.messagesystem

import gov.cdc.ocio.processingstatusapi.models.MessageSystemType
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the unsupported messaging service health checks.
 */
class HealthCheckUnsupportedMessageSystem(
    private val messageSystem: String?
) : HealthCheckSystem("Messaging Service") {

    /**
     * No health check - just inform unsupported
     */
    override fun doHealthCheck(): HealthCheckResult {
        val healthIssue = if (messageSystem != null) {
            "Unsupported message system: $messageSystem"
        } else {
            val options = MessageSystemType.entries.map { it.name }
            "MSG_SYSTEM environment variable not provided (available options are $options)."
        }

        return HealthCheckResult(
            service,
            HealthStatusType.STATUS_DOWN,
            healthIssues = healthIssue
        )
    }
}