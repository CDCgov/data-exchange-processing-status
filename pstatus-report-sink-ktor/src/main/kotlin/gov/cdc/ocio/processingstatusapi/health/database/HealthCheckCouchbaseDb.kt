package gov.cdc.ocio.processingstatusapi.health.database

import gov.cdc.ocio.processingstatusapi.health.HealthCheckSystem
import org.koin.core.component.KoinComponent

/**
 * Concrete implementation of the couchbase health check.
 */
class HealthCheckCouchbaseDb: HealthCheckSystem("Couchbase DB"), KoinComponent {
    override fun doHealthCheck() {
        TODO("Not yet implemented")
    }
}