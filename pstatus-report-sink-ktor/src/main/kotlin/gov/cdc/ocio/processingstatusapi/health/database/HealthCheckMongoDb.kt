package gov.cdc.ocio.processingstatusapi.health.database

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.mongodb.*
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import gov.cdc.ocio.database.mongo.MongoConfiguration
import gov.cdc.ocio.processingstatusapi.health.HealthCheck
import gov.cdc.ocio.processingstatusapi.health.HealthCheckSystem
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Concrete implementation of the mongodb health check.
 */
@JsonIgnoreProperties("koin")
class HealthCheckMongoDb: HealthCheckSystem("Mongo DB"), KoinComponent {

    private val config by inject<MongoConfiguration>()

    /**
     * Checks and sets mongo status
     */
    override fun doHealthCheck() {
        try {
            if (isMongoDBHealthy()) {
                status = HealthCheck.STATUS_UP
            }
        } catch (ex: Exception) {
            logger.error("MongoDB is not healthy $ex.message")
            healthIssues = ex.message
        }
    }

    /**
     * Check whether mongo is healthy.
     *
     * @return Boolean
     */
    private fun isMongoDBHealthy(): Boolean {
        return if (connectToDatabase(config.connectionString, config.databaseName) == null)
            throw Exception("Failed to establish a mongo client.")
        else
            true
    }

    /**
     * Connect to monogodb with the provided URI and database name.
     *
     * @param uri String
     * @param databaseName String
     * @return MongoDatabase?
     */
    private fun connectToDatabase(uri: String, databaseName: String): MongoDatabase? {

        // Construct a ServerApi instance using the ServerApi.builder() method
        val serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build()

        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(uri))
            .serverApi(serverApi)
            .build()

        val mongoClient = MongoClients.create(settings)

        val database = mongoClient.getDatabase(databaseName)
        try {
            // Send a ping to confirm a successful connection
            val command = BsonDocument("ping", BsonInt64(1))
            val commandResult = database.runCommand(command)
            logger.info("Pinged the mongo db and successfully connected! commandResult = $commandResult")
            return database
        } catch (ex: MongoException) {
            logger.error("Failed to connect to mongodb with exception: ${ex.message}")
        }

        return null
    }
}