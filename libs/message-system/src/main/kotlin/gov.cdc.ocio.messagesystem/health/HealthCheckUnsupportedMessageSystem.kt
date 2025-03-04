package gov.cdc.ocio.messagesystem.health

import gov.cdc.ocio.messagesystem.models.MessageSystemType
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the unsupported messaging service health checks.
 */
class HealthCheckUnsupportedMessageSystem(
    system: String,
    private val messageSystem: String?
) : HealthCheckSystem(system, "Messaging Service") {

    /**
     * No health check - just inform unsupported
     */
    override fun doHealthCheck(): HealthCheckResult {
        val options = MessageSystemType.entries.map { it.name }
        val healthIssue = if (messageSystem != null) {
            "Unsupported message system: $messageSystem.  Available options are $options."
        } else {
            "MSG_SYSTEM environment variable not provided.  Available options are $options."
        }

        return HealthCheckResult(
            system,
            service,
            HealthStatusType.STATUS_DOWN,
            healthIssues = healthIssue
        )
    }
}