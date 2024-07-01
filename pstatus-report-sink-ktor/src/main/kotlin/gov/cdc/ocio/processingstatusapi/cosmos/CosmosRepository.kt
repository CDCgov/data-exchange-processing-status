package gov.cdc.ocio.processingstatusapi.cosmos

import org.koin.core.component.KoinComponent

class CosmosRepository(uri: String, authKey: String, reportsContainerName: String, partitionKey: String): KoinComponent {

    val reportsContainer = CosmosContainerManager.initDatabaseContainer(uri, authKey, reportsContainerName, partitionKey)!!

}