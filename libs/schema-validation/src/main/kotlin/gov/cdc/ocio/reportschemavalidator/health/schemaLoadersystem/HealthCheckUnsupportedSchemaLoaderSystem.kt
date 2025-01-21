
package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem

import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the unsupported messaging service health checks.
 */
class HealthCheckUnsupportedSchemaLoaderSystem(
    private val schemaLoaderName: String
) : HealthCheckSystem("Schema Loader") {

    /**
     * No health check - just inform unsupported
     */
    override fun doHealthCheck(): HealthCheckResult {
        return HealthCheckResult(
            service,
            HealthStatusType.STATUS_DOWN,
            "Unsupported schema loader: $schemaLoaderName")
    }
}
