package gov.cdc.ocio.processingstatusapi.functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
//import gov.cdc.ocio.processingstatusapi.cosmos.CosmosClientManager
import java.util.*


class HealthCheckFunction {

    fun run(
        request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext
    ): HttpResponseMessage {
    
//        try {
//            val cosmosClient = CosmosClientManager.getCosmosClient()
//
//            val databaseName = System.getenv("CosmosDbDatabaseName")
//            val containerName = System.getenv("CosmosDbContainerName")
//
//            val cosmosDB = cosmosClient.getDatabase(databaseName)
//            val container = cosmosDB.getContainer(containerName)
//
//            val sqlQuery = "select * from $containerName t OFFSET 0 LIMIT 1"
//            val items = container.queryItems(
//                sqlQuery, CosmosQueryRequestOptions(),
//                Item::class.java
//            )

            return request
                .createResponseBuilder(HttpStatus.OK)
                .build()

//        } catch (ex: Throwable) {
//
//            return request
//                .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
//                .build()
//        }
    }
}