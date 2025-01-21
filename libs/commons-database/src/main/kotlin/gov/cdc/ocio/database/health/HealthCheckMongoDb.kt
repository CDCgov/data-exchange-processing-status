package gov.cdc.ocio.database.health

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.bson.BsonDocument
import org.bson.BsonInt64


/**
 * Concrete implementation of the MongoDB health check.
 */
class HealthCheckMongoDb(
    private val mongoClient: MongoClient,
    private val databaseName: String
) : HealthCheckSystem("Mongo DB") {

    /**
     * Checks and sets MongoDB status.
     *
     * @return HealthCheckResult
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = isMongoDBHealthy()
        result.onFailure { error ->
            val reason = "MongoDB is not healthy: ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(service, HealthStatusType.STATUS_UP)
    }

    /**
     * Checks whether MongoDB is healthy.
     *
     * @return Boolean
     */
    private fun isMongoDBHealthy(): Result<Boolean> {
        return try {
            val database = connectToDatabase()
            if (database == null) {
                Result.failure(Exception("Failed to establish a MongoDB connection."))
            } else {
                logger.info("MongoDB connection is healthy.")
                Result.success(true)
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    /**
     * Connects to MongoDB and pings the database.
     *
     * @return MongoDatabase?
     */
    private fun connectToDatabase(): MongoDatabase? {
        val database = mongoClient.getDatabase(databaseName)
        return try {
            // Send a ping to confirm a successful connection
            val command = BsonDocument("ping", BsonInt64(1))
            database.runCommand(command)
            logger.info("Successfully pinged MongoDB database: $databaseName")
            database
        } catch (ex: Exception) {
            logger.error("Failed to ping MongoDB: ${ex.message}")
            null
        }
    }
}
