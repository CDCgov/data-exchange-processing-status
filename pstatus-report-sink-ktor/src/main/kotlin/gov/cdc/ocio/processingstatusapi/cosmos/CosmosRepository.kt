package gov.cdc.ocio.processingstatusapi.cosmos

/**
 * The class which initializes and creates an instance of a cosmos db reports container
 * @param uri :String
 * @param authKey:String
 * @param reportsContainerName:String
 * @param partitionKey:String
 *
 */
class CosmosRepository(uri: String, authKey: String, reportsContainerName: String, partitionKey: String) {
    val reportsContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsContainerName, partitionKey)
}

/**
 * The class which initializes and creates an instance of a cosmos db reports deadletter container
 * @param uri :String
 * @param authKey:String
 * @param reportsContainerName:String
 * @param partitionKey:String
 *
 */
class CosmosDeadLetterRepository(uri: String, authKey: String, reportsContainerName: String, partitionKey: String) {
    val reportsDeadLetterContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsContainerName, partitionKey)
}