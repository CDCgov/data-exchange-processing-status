package gov.cdc.ocio.processingstatusapi.cosmos

import org.koin.core.component.KoinComponent

class CosmosRepository(reportsContainerName: String, partitionKey: String): KoinComponent {

    val reportsContainer = CosmosContainerManager.initDatabaseContainer(reportsContainerName, partitionKey)!!

}