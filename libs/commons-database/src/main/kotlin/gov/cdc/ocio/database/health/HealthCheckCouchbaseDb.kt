package gov.cdc.ocio.database.health

import com.couchbase.client.java.Cluster
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.database.couchbase.CouchbaseConfiguration
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Concrete implementation of the couchbase health check.
 */
@JsonIgnoreProperties("koin")
class HealthCheckCouchbaseDb(
    private val couchbaseCluster: Cluster? = null
) : HealthCheckSystem("Couchbase DB"), KoinComponent {

    private val config by inject<CouchbaseConfiguration>()

    // Lazily initialized Couchbase Cluster instance
    private val defaultCluster: Cluster by lazy {
        Cluster.connect(config.connectionString, config.username, config.password)
    }

    /**
     * Checks and sets couchbase status
     *
     * @return HealthCheckResult
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = isCouchbaseDbHealthy()
        result.onFailure { error ->
            val reason = "Couchbase DB is not healthy: ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(service, HealthStatusType.STATUS_UP)
    }

    /**
     * Checks whether the couchbase database is reachable.
     *
     * @return Result<Boolean>
     */
    private fun isCouchbaseDbHealthy(): Result<Boolean> {
        return try {
            val cluster = couchbaseCluster ?: defaultCluster // Use injected or default cluster
            // Perform a lightweight health check like a ping
            if (!cluster.ping().id().isNullOrEmpty())
                Result.success(true)
            else
                Result.failure(Exception("Established connection to Couchbase DB, but ping failed."))
        } catch (e: Exception) {
            throw Exception("Failed to establish connection to Couchbase DB.")
        }
    }
}