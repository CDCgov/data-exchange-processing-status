package gov.cdc.ocio.processingstatusapi.couchbase

import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Scope
import gov.cdc.ocio.processingstatusapi.mongo.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.persistence.Collection
import gov.cdc.ocio.processingstatusapi.persistence.CouchbaseCollection
import java.time.Duration


/**
 *
 * @property cluster (Cluster..Cluster?)
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 * @constructor
 */
class CouchbaseRepository(connectionString: String, username: String, password: String): ProcessingStatusRepository() {

    // Connect without customizing the cluster environment
    private var cluster = Cluster.connect(connectionString, username, password)

    private val processingStatusBucket = cluster.bucket("ProcessingStatus")

    private val scope: Scope

    private val reportsCouchbaseCollection: com.couchbase.client.java.Collection

    private val reportsDeadLetterCouchbaseCollection: com.couchbase.client.java.Collection

    init {
        processingStatusBucket.waitUntilReady(Duration.ofSeconds(10))

        scope = processingStatusBucket.scope("data")

        reportsCouchbaseCollection = scope.collection("Reports")

        reportsDeadLetterCouchbaseCollection = scope.collection("Reports-DeadLetter")
    }

    override var reportsCollection = CouchbaseCollection(scope, reportsCouchbaseCollection) as Collection

    override var reportsDeadLetterCollection = CouchbaseCollection(scope, reportsDeadLetterCouchbaseCollection) as Collection

}