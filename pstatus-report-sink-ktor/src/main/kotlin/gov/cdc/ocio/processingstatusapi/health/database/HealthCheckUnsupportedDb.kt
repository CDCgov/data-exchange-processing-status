package gov.cdc.ocio.processingstatusapi.health.database

import gov.cdc.ocio.processingstatusapi.health.HealthCheck
import gov.cdc.ocio.processingstatusapi.health.HealthCheckSystem


/**
 * Concrete implementation of the unsupported messaging service health checks.
 */
class HealthCheckUnsupportedDb : HealthCheckSystem("Database") {

    /**
     * No health check - just inform unsupported
     */
    override fun doHealthCheck() {
        status = HealthCheck.STATUS_UNSUPPORTED
    }
}