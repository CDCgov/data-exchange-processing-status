package gov.cdc.ocio.database.health

import gov.cdc.ocio.database.DatabaseType
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the unsupported messaging service health checks.
 */
class HealthCheckUnsupportedDb(
    system: String,
    private val databaseName: String?
) : HealthCheckSystem(system, null) {

    /**
     * No health check - just inform unsupported
     */
    override fun doHealthCheck(): HealthCheckResult {
        val options = DatabaseType.entries.map { it.name }
        val healthIssue = if (databaseName != null) {
            "Unsupported database: $databaseName.  Available options are $options."
        } else {
            "DATABASE environment variable not provided.  Available options are $options."
        }

        return HealthCheckResult(
            system,
            service,
            HealthStatusType.STATUS_DOWN,
            healthIssues = healthIssue
        )
    }
}