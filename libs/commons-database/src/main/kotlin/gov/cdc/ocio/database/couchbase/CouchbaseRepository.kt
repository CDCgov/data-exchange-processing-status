package gov.cdc.ocio.database.couchbase

import gov.cdc.ocio.database.persistence.Collection
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Scope
import com.couchbase.client.java.transactions.Transactions
import gov.cdc.ocio.database.health.HealthCheckCouchbaseDb
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.types.health.HealthCheckSystem
import mu.KotlinLogging
import java.time.Duration


/**
 * Couchbase implementation of the processing status repository.
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
class CouchbaseRepository(
    connectionString: String,
    username: String,
    password: String,
    bucketName: String = "ProcessingStatus",
    scopeName: String = "data",
    reportsCollectionName: String = "Reports",
    reportsDeadLetterCollectionName: String = "Reports-DeadLetter",
    notificationSubscriptionsCollectionName: String = "NotificationSubscriptions"
) : ProcessingStatusRepository() {

    private val logger = KotlinLogging.logger {}

    /**
     * Lazily instantiate Couchbase transactions. This way, you do not need to pass a transactions
     * instance into the constructor.
     */


    // Connect without customizing the cluster environment
    private var cluster = Cluster.connect(connectionString, username, password)

    /**
     * Lazily obtain the Transactions instance from the Cluster.
     * This ensures that the Transactions object is created only when first needed.
     */
    private val transactions: Transactions by lazy {
        cluster.transactions()  // Assumes that your Cluster instance has a `transactions()` method.
    }

    private val processingStatusBucket = cluster.bucket(bucketName)

    private val scope: Scope

    private val reportsCouchbaseCollection: com.couchbase.client.java.Collection

    private val reportsDeadLetterCouchbaseCollection: com.couchbase.client.java.Collection

    private val notificationSubscriptionsCouchbaseCollection: com.couchbase.client.java.Collection

    init {
        val result = runCatching {
            processingStatusBucket.waitUntilReady(Duration.ofSeconds(10))
        }
        result.onFailure {
            logger.error("Failed to establish an initial connection to Couchbase!")
        }

        scope = processingStatusBucket.scope(scopeName)

        reportsCouchbaseCollection = scope.collection(reportsCollectionName)

        reportsDeadLetterCouchbaseCollection = scope.collection(reportsDeadLetterCollectionName)

        notificationSubscriptionsCouchbaseCollection = scope.collection(notificationSubscriptionsCollectionName)
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

    override var notificationSubscriptionsCollection =
        CouchbaseCollection(
            reportsDeadLetterCollectionName,
            scope,
            notificationSubscriptionsCouchbaseCollection
        ) as Collection

    override var healthCheckSystem = HealthCheckCouchbaseDb(system) as HealthCheckSystem

    /**
     * Convenience method to run an action within a transaction.
     */
    /**
     * When we need transaction in the future we can use this function
     */
   /* fun runTransaction(action: (TransactionAttemptContext) -> Unit) {
              transactions.run({ ctx ->
            action(ctx)
        },
            TransactionOptions.transactionOptions().timeout(Duration.ofSeconds(30))) // Increase timeout to 30s)
           TransactionOptions.transactionOptions().durabilityLevel(DurabilityLevel.NONE)

    }*/
}