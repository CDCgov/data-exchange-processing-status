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
class HealthCheckCouchbaseDb(private val couchbaseCluster: Cluster? = null) : HealthCheckSystem("Couchbase DB"), KoinComponent {

    private val config by inject<CouchbaseConfiguration>()

    // Lazily initialized Couchbase Cluster instance
    private val defaultCluster: Cluster by lazy {
        Cluster.connect(config.connectionString, config.username, config.password)
    }

    /**
     * Checks and sets couchbase status
     */
    override fun doHealthCheck() {
        try {
            val cluster = couchbaseCluster ?: defaultCluster // Use injected or default cluster
            cluster.ping() // Perform a lightweight health check like a ping
            status = HealthStatusType.STATUS_UP

        } catch (ex: Exception) {
            logger.error("Cosmos DB is not healthy $ex.message")
            healthIssues = ex.message
        }
    }

}