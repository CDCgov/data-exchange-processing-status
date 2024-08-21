package gov.cdc.ocio.processingstatusapi.mongo

import com.mongodb.*
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import gov.cdc.ocio.processingstatusapi.persistence.Collection
import gov.cdc.ocio.processingstatusapi.persistence.MongoCollection
import org.bson.BsonDocument
import org.bson.BsonInt64


/**
 * Base class for all processing status repositories
 *
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 */
open class ProcessingStatusRepository {

    // Common interface for the reports collection
    open lateinit var reportsCollection: Collection

    // Common interface for the reports deadletter collection
    open lateinit var reportsDeadLetterCollection: Collection
}

/**
 * Mongo repository implementation
 *
 * @property mongoClient MongoClient?
 * @property reportsDatabase MongoDatabase?
 * @property reportsMongoCollection MongoCollection<(Document..Document?)>?
 * @property reportsDeadLetterMongoCollection MongoCollection<(Document..Document?)>?
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 * @constructor
 */
class MongoRepository(uri: String, databaseName: String): ProcessingStatusRepository() {

    private var mongoClient: MongoClient? = null

    private val reportsDatabase = connectToDatabase(uri, databaseName)

    private val reportsMongoCollection = reportsDatabase?.getCollection("Reports")
    private val reportsDeadLetterMongoCollection = reportsDatabase?.getCollection("Reports-DeadLetter")

    override var reportsCollection = reportsMongoCollection?.let {
        MongoCollection(it)
    } as Collection

    override var reportsDeadLetterCollection = reportsDeadLetterMongoCollection?.let {
        MongoCollection(it)
    } as Collection

    private fun connectToDatabase(uri: String, databaseName: String): MongoDatabase? {

        // Construct a ServerApi instance using the ServerApi.builder() method
        val serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build()

        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(uri))
            .serverApi(serverApi)
            .build()

        mongoClient = MongoClients.create(settings)

        val database = mongoClient?.getDatabase(databaseName)
        try {
            // Send a ping to confirm a successful connection
            val command = BsonDocument("ping", BsonInt64(1))
            val commandResult = database?.runCommand(command)
            println("Pinged your deployment. You successfully connected to MongoDB! commandResult = $commandResult")
            return database
        } catch (me: MongoException) {
            System.err.println(me)
        }

        return null
    }

}