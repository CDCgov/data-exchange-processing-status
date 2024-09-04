package gov.cdc.ocio.processingstatusapi.persistence

import com.google.gson.Gson
import org.bson.Document

/**
 * MongoDB Collection implementation
 *
 * @property mongoCollection MongoCollection<Document>
 * @constructor
 */
class MongoCollection(private val mongoCollection: com.mongodb.client.MongoCollection<Document>): Collection {

    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        TODO("Not yet implemented")
    }

    override fun <T> createItem(id: String, item: T, classType: Class<T>, partitionKey: String?): Boolean {
        val document = Document.parse(Gson().toJson(item))
        val result = mongoCollection.insertOne(document)
        return result.wasAcknowledged()
    }

    override fun deleteItem(itemId: String?, partitionKey: String?): Any {
        TODO("Not yet implemented")
    }

}