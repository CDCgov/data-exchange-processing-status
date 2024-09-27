package gov.cdc.ocio.processingstatusapi.cosmos

import gov.cdc.ocio.processingstatusapi.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.persistence.Collection


/**
 * The class which initializes and creates an instance of a cosmos db reports and reports-deadletter containers.
 *
 * @property reportsContainerName String
 * @property reportsDeadLetterContainerName String
 * @property reportsContainer CosmosContainer?
 * @property reportsDeadLetterContainer CosmosContainer?
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 * @constructor
 */
class CosmosRepository(
    uri: String,
    authKey: String,
    partitionKey: String
) : ProcessingStatusRepository() {

    private val reportsContainerName = "Reports"

    private val reportsDeadLetterContainerName = "Reports-DeadLetter"

    private val reportsContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsContainerName, partitionKey)

    private val reportsDeadLetterContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsDeadLetterContainerName, partitionKey)

    override var reportsCollection = CosmosCollection(reportsContainerName, reportsContainer) as Collection

    override var reportsDeadLetterCollection =
        CosmosCollection(reportsDeadLetterContainerName, reportsDeadLetterContainer) as Collection
}
