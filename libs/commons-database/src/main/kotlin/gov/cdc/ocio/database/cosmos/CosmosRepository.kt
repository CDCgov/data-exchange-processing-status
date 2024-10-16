package gov.cdc.ocio.database.cosmos

import gov.cdc.ocio.database.persistence.Collection
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository


/**
 * The class which initializes and creates an instance of a cosmos db reports and reports-deadletter containers.
 *
 * @param uri[String] URI of the CosmosDB to connect with.
 * @param authKey[String] Authorization Key to use when connecting to the CosmosDB.
 * @param partitionKey[String] Container partition key to use, which defaults to "/uploadId" if not provided.
 * @param containerName[String] Reports, Rules container name to use, which defaults to "Reports" if not provided.
 * @param reportsDeadLetterContainerName[String] Reports dead letter container name to use, which default to
 * "Reports-DeadLetter" if not provided.
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
    containerName: String = "Reports",
    reportsDeadLetterContainerName: String = "Reports-DeadLetter"
) : ProcessingStatusRepository() {

    private val reportsContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, containerName, partitionKey)

    private val reportsDeadLetterContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsDeadLetterContainerName, partitionKey)

    private val rulesContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, containerName, partitionKey)

    override var reportsCollection = CosmosCollection(containerName, reportsContainer) as Collection

    override var reportsDeadLetterCollection =
        CosmosCollection(reportsDeadLetterContainerName, reportsDeadLetterContainer) as Collection

    override var rulesCollection = CosmosCollection(containerName, rulesContainer) as Collection
}
