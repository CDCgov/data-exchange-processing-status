package gov.cdc.ocio.processingstatusapi.couchbase

import com.couchbase.client.java.Scope
import com.couchbase.client.java.json.JsonObject
import com.google.gson.GsonBuilder
import gov.cdc.ocio.processingstatusapi.persistence.Collection
import gov.cdc.ocio.processingstatusapi.utils.DateLongFormatTypeAdapter
import java.util.*


/**
 * Couchbase Collection implementation
 *
 * @property couchbaseCollection Collection
 * @constructor
 */
class CouchbaseCollection(
    private val couchbaseScope: Scope,
    private val couchbaseCollection: com.couchbase.client.java.Collection
): Collection {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateLongFormatTypeAdapter())
        .create()

    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        val result = couchbaseScope.query(query)
        val jsonObjects: List<JsonObject> = result.rowsAsObject()
        val results = mutableListOf<T>()
        jsonObjects.forEach {
            // Couchbase items are received as an array, but within each array element is a field with the content.
            // The content field name will match that of the collection name.
            val collectionName = it.names.first()
            val item = it.get(collectionName)
            val obj = gson.fromJson(item.toString(), classType)
            results.add(obj)
        }
        return results
    }

    override fun <T> createItem(id: String, item: T, classType: Class<T>, partitionKey: String?): Boolean {
        val upsertResult = couchbaseCollection.upsert(
            id,
            item
        )
        return upsertResult != null
    }

    override fun deleteItem(itemId: String?, partitionKey: String?): Boolean {
        val removeResult = couchbaseCollection.remove(itemId)
        return removeResult != null
    }

}