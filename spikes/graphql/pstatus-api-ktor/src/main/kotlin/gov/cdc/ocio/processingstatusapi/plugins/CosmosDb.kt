package gov.cdc.ocio.processingstatusapi.plugins

import com.azure.cosmos.*
import com.azure.cosmos.models.CosmosContainerProperties
import com.azure.cosmos.models.ThroughputProperties
import io.ktor.server.application.*
import io.ktor.server.config.*
import mu.KotlinLogging

class AzureCosmosDbConfiguration(config: ApplicationConfig) {
    var cosmosDbEndpoint: String = config.tryGetString("client.endpoint") ?: ""
    var cosmosDbKey: String = config.tryGetString("client.key") ?: ""
    var databaseName: String = config.tryGetString("database_name") ?: ""
    var containerName: String = config.tryGetString("container_name") ?: ""
}

val AzureCosmosDb = createApplicationPlugin(
    name = "AzureCosmosDb",
    configurationPath = "azure.cosmos_db",
    createConfiguration = ::AzureCosmosDbConfiguration) {

    val cosmosDbEndpoint = pluginConfig.cosmosDbEndpoint
    val cosmosDbKey = pluginConfig.cosmosDbKey
    val databaseName = pluginConfig.databaseName
    val containerName = pluginConfig.containerName

    val cosmosClient by lazy {
        CosmosClientBuilder()
            .endpoint(cosmosDbEndpoint)
            .key(cosmosDbKey)
            .consistencyLevel(ConsistencyLevel.EVENTUAL)
            .contentResponseOnWriteEnabled(true)
            .buildClient()
    }

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

            // setup database
            logger.info("calling createDatabaseIfNotExists...")
            val db = createDatabaseIfNotExists(cosmosClient, "ProcessingStatus")!!

            val containerProperties = CosmosContainerProperties(containerName, partitionKey)

            // Provision throughput
            val throughputProperties = ThroughputProperties.createAutoscaledThroughput(1000)

            //  Create container with 1000 RU/s
            logger.info("calling createContainerIfNotExists...")
            val databaseResponse = db.createContainerIfNotExists(containerProperties, throughputProperties)

            return db.getContainer(databaseResponse.properties.id)

        } catch (ex: CosmosException) {
            logger.error("exception: ${ex.localizedMessage}")
        }
        return null
    }
}