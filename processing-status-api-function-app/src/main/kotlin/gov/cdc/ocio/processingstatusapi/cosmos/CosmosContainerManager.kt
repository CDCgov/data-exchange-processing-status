package gov.cdc.ocio.processingstatusapi.cosmos

import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosDatabase
import com.azure.cosmos.CosmosException
import com.azure.cosmos.models.CosmosContainerProperties
import com.azure.cosmos.models.ThroughputProperties
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.cosmossync.cosmos.CosmosClientManager

class CosmosContainerManager {

    companion object {

        @Throws(Exception::class)
        fun createDatabaseIfNotExists(context: ExecutionContext, cosmosClient: CosmosClient, databaseName: String): CosmosDatabase? {
            context.logger.info("Create database $databaseName if not exists...")

            //  Create database if not exists
            val databaseResponse = cosmosClient.createDatabaseIfNotExists(databaseName)
            return cosmosClient.getDatabase(databaseResponse.properties.id)
        }

        fun initDatabaseContainer(context: ExecutionContext, containerName: String): CosmosContainer? {
            try {
                val logger = context.logger

                logger.info("calling getCosmosClient...")
                val cosmosClient = CosmosClientManager.getCosmosClient()

                // setup database
                logger.info("calling createDatabaseIfNotExists...")
                val db = createDatabaseIfNotExists(context, cosmosClient, "UploadStatus")!!

                val containerProperties = CosmosContainerProperties(containerName, "/partitionKey")

                // Provision throughput
                val throughputProperties = ThroughputProperties.createManualThroughput(400)

                //  Create container with 400 RU/s
                logger.info("calling createContainerIfNotExists...")
                val databaseResponse = db.createContainerIfNotExists(containerProperties, throughputProperties)

                return db.getContainer(databaseResponse.properties.id)

            } catch (ex: CosmosException) {
                context.logger.info("exception: ${ex.localizedMessage}")
            }
            return null
        }
    }
}