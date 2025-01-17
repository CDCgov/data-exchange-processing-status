package gov.cdc.ocio.database.health

import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the unsupported messaging service health checks.
 */
class HealthCheckUnsupportedDb(
    private val databaseName: String
) : HealthCheckSystem("Database") {

    /**
     * No health check - just inform unsupported
     */
    override fun doHealthCheck(): HealthCheckResult {
        return HealthCheckResult(
            service,
            HealthStatusType.STATUS_DOWN,
            healthIssues = "Unsupported database: $databaseName")
    }
}