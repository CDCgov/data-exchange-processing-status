package gov.cdc.ocio.processingstatusapi.cosmos

import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosDatabase
import com.azure.cosmos.CosmosException
import com.azure.cosmos.models.CosmosContainerProperties
import com.azure.cosmos.models.ThroughputProperties
import mu.KotlinLogging

class CosmosContainerManager {

    companion object {

        @Throws(Exception::class)
        fun createDatabaseIfNotExists(cosmosClient: CosmosClient, databaseName: String): CosmosDatabase? {
            val logger = KotlinLogging.logger {}
            logger.info("Create database $databaseName if not exists...")
            //  Create database if not exists
            val databaseResponse = cosmosClient.createDatabaseIfNotExists(databaseName)
            return cosmosClient.getDatabase(databaseResponse.properties.id)
        }

        fun initDatabaseContainer(containerName: String, partitionKey: String): CosmosContainer? {
            val logger = KotlinLogging.logger {}
            try {
                logger.info("calling getCosmosClient...")
                val cosmosClient = CosmosClientManager.getCosmosClient()

                // setup database
                logger.info("calling createDatabaseIfNotExists...")
                val db = createDatabaseIfNotExists(cosmosClient, "ProcessingStatus")!!

                val containerProperties = CosmosContainerProperties(containerName, partitionKey)

                // Provision throughput
                val throughputProperties = ThroughputProperties.createManualThroughput(400)

                //  Create container with 400 RU/s
                logger.info("calling createContainerIfNotExists...")
                val databaseResponse = db.createContainerIfNotExists(containerProperties, throughputProperties)

                return db.getContainer(databaseResponse.properties.id)

            } catch (ex: CosmosException) {
                logger.error("exception: ${ex.localizedMessage}")
            }
            return null
        }
    }
}