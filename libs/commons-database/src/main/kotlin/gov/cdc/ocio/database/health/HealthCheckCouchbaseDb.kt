package gov.cdc.ocio.database.health

import com.couchbase.client.java.Cluster
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.database.couchbase.CouchbaseConfiguration
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Concrete implementation of the couchbase health check.
 */
@JsonIgnoreProperties("koin")
class HealthCheckCouchbaseDb: HealthCheckSystem("Couchbase DB"), KoinComponent {

    private val config by inject<CouchbaseConfiguration>()

    /**
     * Checks and sets couchbase status
     */
    override fun doHealthCheck() {
        try {
            if (isCouchbaseDBHealthy()) {
                status = HealthStatusType.STATUS_UP
            }
        } catch (ex: Exception) {
            logger.error("Cosmos DB is not healthy $ex.message")
            healthIssues = ex.message
        }
    }

    /**
     * Check whether couchbase is healthy.
     *
     * @return Boolean
     */
    private fun isCouchbaseDBHealthy(): Boolean {
        return if (Cluster.connect(config.connectionString, config.username, config.password) == null)
            throw Exception("Failed to establish a couchbase client.")
        else
            true
    }
}