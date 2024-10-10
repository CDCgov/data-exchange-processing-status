package gov.cdc.ocio.database.health

import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the unsupported messaging service health checks.
 */
class HealthCheckUnsupportedDb : HealthCheckSystem("Database") {

    /**
     * No health check - just inform unsupported
     */
    override fun doHealthCheck() {
        status = HealthStatusType.STATUS_UNSUPPORTED
    }
}