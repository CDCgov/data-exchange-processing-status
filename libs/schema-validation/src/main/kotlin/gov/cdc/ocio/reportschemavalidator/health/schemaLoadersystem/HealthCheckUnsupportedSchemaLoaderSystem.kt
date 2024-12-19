
package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem

import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType



/**
 * Concrete implementation of the unsupported messaging service health checks.
 */

class HealthCheckUnsupportedSchemaLoaderSystem : HealthCheckSystem("Cloud Schema Loader") {


/**
     * No health check - just inform unsupported
     */

    override fun doHealthCheck() {
        status = HealthStatusType.STATUS_UNSUPPORTED
    }
}
