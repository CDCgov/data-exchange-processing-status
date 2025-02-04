package gov.cdc.ocio.database.cosmos

import gov.cdc.ocio.database.health.HealthCheckCosmosDb
import gov.cdc.ocio.database.persistence.Collection
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * The class which initializes and creates an instance of a cosmos db reports and reports-deadletter containers.
 *
 * @param uri[String] URI of the CosmosDB to connect with.
 * @param authKey[String] Authorization Key to use when connecting to the CosmosDB.
 * @param partitionKey[String] Container partition key to use, which defaults to "/uploadId" if not provided.
 * @param reportsContainerName[String] Reports, Rules container name to use, which defaults to "Reports" if not provided.
 * @param reportsDeadLetterContainerName[String] Reports dead letter container name to use, which defaults to
 * "Reports-DeadLetter" if not provided.
 * @param notificationSubscriptionsContainerName[String] Notification subscriptions container name to use, which
 * defaults to "NotificationSubscriptions" if not provided.
 * @property reportsContainer[com.azure.cosmos.CosmosContainer]?
 * @property reportsDeadLetterContainer[com.azure.cosmos.CosmosContainer]?
 * @property reportsCollection[Collection]
 * @property reportsDeadLetterCollection[Collection]
 * @constructor Provides a CosmosDB repository, which is a concrete implementation of the [ProcessingStatusRepository]
 *
 * @see [ProcessingStatusRepository]
 */
class CosmosRepository(
    uri: String,
    authKey: String,
    partitionKey: String = "/uploadId",
    reportsContainerName: String = "Reports",
    reportsDeadLetterContainerName: String = "Reports-DeadLetter",
    notificationSubscriptionsContainerName: String = "NotificationSubscriptions"
) : ProcessingStatusRepository() {

    private val reportsContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsContainerName, partitionKey)

    private val reportsDeadLetterContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsDeadLetterContainerName, partitionKey)

    private val subscriptionManagementContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, notificationSubscriptionsContainerName, partitionKey)

    override var reportsCollection = CosmosCollection(reportsContainer) as Collection

    override var reportsDeadLetterCollection =
        CosmosCollection(reportsDeadLetterContainer) as Collection

    override var subscriptionManagementCollection =
        CosmosCollection(subscriptionManagementContainer) as Collection

    override var healthCheckSystem = HealthCheckCosmosDb(
        system,
        CosmosClientManager.getCosmosClient(uri, authKey)!!
    ) as HealthCheckSystem
}
