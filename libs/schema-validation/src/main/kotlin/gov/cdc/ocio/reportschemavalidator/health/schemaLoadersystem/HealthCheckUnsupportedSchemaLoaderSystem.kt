
package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem

import gov.cdc.ocio.reportschemavalidator.utils.SchemaLoaderSystemType
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the unsupported messaging service health checks.
 */
class HealthCheckUnsupportedSchemaLoaderSystem(
    system: String,
    private val schemaLoaderName: String?
) : HealthCheckSystem(system, null) {

    /**
     * No health check - just inform unsupported
     */
    override fun doHealthCheck(): HealthCheckResult {
        val options = SchemaLoaderSystemType.entries.map { it.name }
        val healthIssue = if (schemaLoaderName != null) {
            "Unsupported schema loader: $schemaLoaderName.  Available options are $options."
        } else {

            "REPORT_SCHEMA_LOADER_SYSTEM environment variable not provided.  Available options are $options."
        }

        return HealthCheckResult(
            system,
            service,
            HealthStatusType.STATUS_DOWN,
            healthIssues = healthIssue
        )
    }
}
