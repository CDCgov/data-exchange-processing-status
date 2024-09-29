package gov.cdc.ocio.database.mongo

import gov.cdc.ocio.database.persistence.Collection
import com.google.gson.Gson
import org.bson.Document


/**
 * MongoDB Collection implementation
 *
 * @property mongoCollection MongoCollection<Document>
 * @constructor
 */
class MongoCollection(private val mongoCollection: com.mongodb.client.MongoCollection<Document>): Collection {

    /**
     * Execute the provided query and return the results as POJOs.
     *
     * @param query String?
     * @param classType Class<T>?
     * @return List<T>
     */
    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        TODO("Not yet implemented")
    }

    /**
     * Create a dynamodb item from the provided data.
     *
     * @param id String
     * @param item T
     * @param classType Class<T>
     * @param partitionKey String?
     * @return Boolean
     */
    override fun <T> createItem(id: String, item: T, classType: Class<T>, partitionKey: String?): Boolean {
        val document = Document.parse(Gson().toJson(item))
        val result = mongoCollection.insertOne(document)
        return result.wasAcknowledged()
    }

    /**
     * Delete the specified item id.
     *
     * @param itemId String?
     * @param partitionKey String?
     * @return Boolean
     */
    override fun deleteItem(itemId: String?, partitionKey: String?): Boolean {
        TODO("Not yet implemented")
    }

    override val collectionVariable = TODO("Not yet implemented")

    override val collectionVariablePrefix = TODO("Not yet implemented")

    override val collectionNameForQuery = TODO("Not yet implemented")

    override val collectionElementForQuery = TODO("Not yet implemented")

}