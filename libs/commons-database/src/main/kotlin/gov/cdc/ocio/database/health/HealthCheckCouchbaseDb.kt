package gov.cdc.ocio.database.health

import com.couchbase.client.core.diagnostics.PingState
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.diagnostics.PingOptions
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.database.couchbase.CouchbaseConfiguration
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration


/**
 * Concrete implementation of the couchbase health check.
 */
@JsonIgnoreProperties("koin")
class HealthCheckCouchbaseDb(
    system: String,
    private val couchbaseCluster: Cluster? = null
) : HealthCheckSystem(system, "Couchbase DB"), KoinComponent {

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
            return HealthCheckResult(system, service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(system, service, HealthStatusType.STATUS_UP)
    }

    /**
     * Checks whether the couchbase database is reachable.
     *
     * @return Result<Boolean>
     */
    private fun isCouchbaseDbHealthy(): Result<Boolean> {
        val cluster = couchbaseCluster ?: defaultCluster // Use injected or default cluster

        val pingResult = runCatching {
            runBlocking {
                waitForCouchbaseConnection(cluster)
            }
        }
        pingResult.onFailure { error ->
            return Result.failure(error)
        }

        return Result.success(true)
    }

    /**
     * Blocking function for attempting to connect to the couchbase database.
     *
     * @param cluster [Cluster]
     * @param retries [Int]
     * @param delayMs [Long]
     * @throws [RuntimeException] - Thrown if unsuccessful.
     */
    @Throws(RuntimeException::class)
    private suspend fun waitForCouchbaseConnection(
        cluster: Cluster,
        retries: Int = 5,
        delayMs: Long = 1000
    ) {
        repeat(retries) { attempt ->
            try {
                // Perform a lightweight health check like a ping
                val timeout = Duration.ofSeconds(2)
                val pingResult = cluster.ping(PingOptions.pingOptions().timeout(timeout))
                if (pingResult.endpoints().isNotEmpty()
                    && pingResult.endpoints().all { (_, endpoints) ->
                        endpoints.all { it.state() == PingState.OK }
                    }) {
                    logger.info("Couchbase connection successful!")
                    return
                }
            } catch (e: Exception) {
                logger.warn("Attempt ${attempt + 1} failed: ${e.message}")
            }
            delay(delayMs)
        }
        throw RuntimeException("Failed to establish Couchbase connection after $retries attempts")
    }
}