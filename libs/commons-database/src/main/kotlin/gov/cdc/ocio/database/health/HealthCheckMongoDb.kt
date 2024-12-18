package gov.cdc.ocio.database.health

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import gov.cdc.ocio.database.mongo.MongoConfiguration
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Concrete implementation of the MongoDB health check.
 */
@JsonIgnoreProperties("koin")
class HealthCheckMongoDb(
    private val mongoClient: MongoClient,
    private val config: MongoConfiguration
) : HealthCheckSystem("Mongo DB"), KoinComponent {

    /**
     * Checks and sets MongoDB status.
     */
    override fun doHealthCheck() {
        try {
            if (isMongoDBHealthy()) {
                status = HealthStatusType.STATUS_UP
            } else {
                throw Exception("MongoDB is not healthy.")
            }
        } catch (ex: Exception) {
            logger.error("MongoDB is not healthy: ${ex.message}")
            status = HealthStatusType.STATUS_DOWN
            healthIssues = ex.message
        }
    }

    /**
     * Checks whether MongoDB is healthy.
     *
     * @return Boolean
     */
    private fun isMongoDBHealthy(): Boolean {
        return try {
            val database = connectToDatabase()
            if (database == null) {
                throw Exception("Failed to establish a MongoDB connection.")
            } else {
                logger.info("MongoDB connection is healthy.")
                true
            }
        } catch (ex: Exception) {
            logger.error("Error during MongoDB health check: ${ex.message}")
            false
        }
    }

    /**
     * Connects to MongoDB and pings the database.
     *
     * @return MongoDatabase?
     */
    private fun connectToDatabase(): MongoDatabase? {
        val database = mongoClient.getDatabase(config.databaseName)
        return try {
            // Send a ping to confirm a successful connection
            val command = BsonDocument("ping", BsonInt64(1))
            database.runCommand(command)
            logger.info("Successfully pinged MongoDB database: ${config.databaseName}")
            database
        } catch (ex: Exception) {
            logger.error("Failed to ping MongoDB: ${ex.message}")
            null
        }
    }
}
