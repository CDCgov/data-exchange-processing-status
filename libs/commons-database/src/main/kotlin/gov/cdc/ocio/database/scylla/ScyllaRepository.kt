package gov.cdc.ocio.database.scylla

import com.datastax.driver.core.Cluster
import gov.cdc.ocio.database.couchbase.CouchbaseCollection
import gov.cdc.ocio.database.persistence.Collection
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import mu.KotlinLogging


/**
 * Scylla implementation of the processing status repository.
 *
 * @param connectionString[String] Connection string for the couchbase database.
 * @param username[String] Username to use when connecting to the database.
 * @param password[String] Password to use when connecting to the database.
 * @param bucketName[String] Name of the bucket within the cluster to use.
 * @param scopeName[String] Name of the scope for the couchbase collections.
 * @param reportsCollectionName[String] Reports collection name to use, which defaults to "Reports" if not provided.
 * @param reportsDeadLetterCollectionName[String] Reports deadletter collection name to use, which defaults to
 * "Reports-DeadLetter" if not provided.
 * @param notificationSubscriptionsCollectionName[String] Notification subscriptions collection name to use, which
 * defaults to "NotificationSubscriptions" if not provided.
 * @property cluster (Cluster..Cluster?)
 * @property processingStatusBucket (Bucket..Bucket?)
 * @property scope Scope
 * @property reportsCouchbaseCollection Collection
 * @property reportsDeadLetterCouchbaseCollection Collection
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 * @constructor Provides a Couchbase repository, which is a concrete implementation of the [ProcessingStatusRepository]
 *
 * @see [ProcessingStatusRepository]
 */
class ScyllaRepository(
    uri: String,
//    username: String,
//    password: String,
//    bucketName: String = "ProcessingStatus",
//    scopeName: String = "data",
    reportsCollectionName: String = "Reports",
//    reportsDeadLetterCollectionName: String = "Reports-DeadLetter",
//    notificationSubscriptionsCollectionName: String = "NotificationSubscriptions"
) : ProcessingStatusRepository() {

    private val logger = KotlinLogging.logger {}

    private val cluster = Cluster.builder()
        .addContactPoint(uri)
//            .withLocalDatacenter("datacenter1")
//            .addContactPoints(
//                "your-node-url.scylla.cloud",
//                "your-node-url.clusters.scylla.cloud",
//                "your-node-url.clusters.scylla.cloud"
//            )
//            .withLoadBalancingPolicy(
//                DCAwareRoundRobinPolicy.builder().withLocalDc("AWS_US_EAST_1").build()
//            ) // your local data center
//            .withAuthProvider(PlainTextAuthProvider("scylla", "your-awesome-password"))
        .build()

    private val session = cluster.connect()

    init {
        logger.info { "Session status: ${session.state}" }
        session.execute("CREATE KEYSPACE IF NOT EXISTS test WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};")
//        session.execute("CREATE TABLE IF NOT EXISTS test.users (id int PRIMARY KEY, name text);")
//        session.execute("INSERT INTO test.users (id, name) VALUES (1, 'John');")

//        val result = session.execute("SELECT * FROM test.users;")
//        result.forEach { row ->
//            logger.info("User: ${row.getInt("id")}, ${row.getString("name")}")
//        }
    }

    override var reportsCollection =
        ScyllaCollection(
            session,
            "test",
            reportsCollectionName
        ) as Collection
}