package gov.cdc.ocio.processingstatusapi.couchbase

import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Scope
import gov.cdc.ocio.processingstatusapi.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.persistence.Collection
import java.time.Duration


/**
 * Couchbase implementation of the processing status repository.
 *
 * @property cluster (Cluster..Cluster?)
 * @property processingStatusBucket (Bucket..Bucket?)
 * @property scope Scope
 * @property reportsCouchbaseCollection Collection
 * @property reportsDeadLetterCouchbaseCollection Collection
 * @property reportsCollectionName String
 * @property reportsDeadLetterCollectionName String
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 * @constructor
 */
class CouchbaseRepository(connectionString: String, username: String, password: String) : ProcessingStatusRepository() {

    // Connect without customizing the cluster environment
    private var cluster = Cluster.connect(connectionString, username, password)

    private val processingStatusBucket = cluster.bucket("ProcessingStatus")

    private val scope: Scope

    private val reportsCouchbaseCollection: com.couchbase.client.java.Collection

    private val reportsDeadLetterCouchbaseCollection: com.couchbase.client.java.Collection

    private val reportsCollectionName = "Reports"

    private val reportsDeadLetterCollectionName = "Reports-DeadLetter"

    init {
        processingStatusBucket.waitUntilReady(Duration.ofSeconds(10))

        scope = processingStatusBucket.scope("data")

        reportsCouchbaseCollection = scope.collection(reportsCollectionName)

        reportsDeadLetterCouchbaseCollection = scope.collection(reportsDeadLetterCollectionName)
    }

    override var reportsCollection =
        CouchbaseCollection(
            reportsCollectionName,
            scope,
            reportsCouchbaseCollection
        ) as Collection

    override var reportsDeadLetterCollection =
        CouchbaseCollection(
            reportsDeadLetterCollectionName,
            scope,
            reportsDeadLetterCouchbaseCollection
        ) as Collection

}