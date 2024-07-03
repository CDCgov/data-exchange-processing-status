package gov.cdc.ocio.processingstatusapi.cosmos

class CosmosRepository(uri: String, authKey: String, reportsContainerName: String, partitionKey: String) {

    val reportsContainer = CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsContainerName, partitionKey)

}

class CosmosDeadLetterRepository(uri: String, authKey: String, reportsContainerName: String, partitionKey: String) {
    val reportsDeadLetterContainer =
        CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsContainerName, partitionKey)
}
