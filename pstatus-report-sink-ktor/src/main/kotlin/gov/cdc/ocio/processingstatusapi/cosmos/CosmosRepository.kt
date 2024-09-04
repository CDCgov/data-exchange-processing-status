package gov.cdc.ocio.processingstatusapi.cosmos

import gov.cdc.ocio.processingstatusapi.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.persistence.Collection
import gov.cdc.ocio.processingstatusapi.persistence.CosmosCollection


/**
 * The class which initializes and creates an instance of a cosmos db reports and reports-deadletter containers.
 *
 * @property reportsContainer CosmosContainer?
 * @property reportsDeadLetterContainer CosmosContainer?
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 * @constructor
 */
class CosmosRepository(uri: String, authKey: String, reportsContainerName: String, partitionKey: String): ProcessingStatusRepository() {

    private val reportsContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsContainerName, partitionKey)

    private val reportsDeadLetterContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsContainerName, partitionKey)

    override var reportsCollection = CosmosCollection(reportsContainer) as Collection

    override var reportsDeadLetterCollection = CosmosCollection(reportsDeadLetterContainer) as Collection
}
