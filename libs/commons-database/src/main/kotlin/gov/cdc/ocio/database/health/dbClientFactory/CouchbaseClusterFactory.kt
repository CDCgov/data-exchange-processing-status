package gov.cdc.ocio.database.health.dbClientFactory

import com.couchbase.client.java.Cluster
import gov.cdc.ocio.database.couchbase.CouchbaseConfiguration

object CouchbaseClusterFactory {
    fun createCluster(config: CouchbaseConfiguration): Cluster {
        return Cluster.connect(config.connectionString, config.username, config.password)
    }
}
