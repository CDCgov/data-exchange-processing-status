package gov.cdc.ocio.database.couchbase

import gov.cdc.ocio.database.persistence.Collection
import com.couchbase.client.java.Scope
import com.couchbase.client.java.json.JsonObject
import com.google.gson.*
import gov.cdc.ocio.database.utils.DateLongFormatTypeAdapter
import gov.cdc.ocio.database.utils.InstantTypeAdapter
import gov.cdc.ocio.database.utils.OffsetDateTimeTypeAdapter
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

/**
 * Couchbase Collection implementation.
 *
 * @param collectionName[String] Collection name associated with this couchbase collection.
 * @property couchbaseScope[Scope] Scope for the couchbase collection, which is defined by the bucket and collection name.
 * @property couchbaseCollection[com.couchbase.client.java.Collection] Couchbase collection associated with this collection.
 * @constructor Creates a couchbase collection for use with the [Collection] interface.
 *
 * @see [CouchbaseRepository]
 * @see [Collection]
 */

class CouchbaseCollection(
    collectionName: String,
    private val couchbaseScope: Scope,
    private val couchbaseCollection: com.couchbase.client.java.Collection
): Collection {

    private val gson = GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .registerTypeAdapter(Date::class.java, DateLongFormatTypeAdapter())
        .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
        .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeTypeAdapter())
        .create()

    /**
     * Execute the provided query and return the results as POJOs.
     *
     * @param query[String]
     * @param classType Class<T>?
     * @return List<T>
     */
    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        val result = couchbaseScope.query(query)
        val results = mutableListOf<T>()
        when (classType) {
            // Handle primitive types
            String::class.java, Boolean::class.java -> {
                results.addAll(result.rowsAs(classType))
            }
            Int::class.java, Long::class.java,Float::class.java -> {
                val expectedResult = result.rowsAs(classType)[0]
                results.add(expectedResult as T)
            }
            // Handle all others as JSON objects
            else -> {
                val jsonObjects = result.rowsAsObject()
                jsonObjects.forEach {
                    // Couchbase items are received as an array, but within each array element is a field with the content.
                    // The content field name will match that of the collection name.
                    val collectionName = it.names.first()
                    val item = if (collectionName.equals(collectionVariable)) it.get(collectionName) else it
                    val obj = gson.fromJson(item.toString(), classType)
                    results.add(obj)
                }
            }
        }
        return results
    }

    /**
     * Create an item from the provided data.
     *
     * @param id String
     * @param item T
     * @param classType Class<T>
     * @param partitionKey String?
     * @return Boolean
     */
    override fun <T> createItem(id: String, item: T, classType: Class<T>, partitionKey: String?): Boolean {
        val upsertResult = couchbaseCollection.upsert(
            id,
            JsonObject.fromJson(gson.toJson(item, classType))
        )
        return upsertResult != null
    }

    /**
     * Delete the specified item from the container.
     *
     * @param itemId String?
     * @param partitionKey String?
     * @return Boolean
     */
    override fun deleteItem(itemId: String?, partitionKey: String?): Boolean {
        val removeResult = couchbaseCollection.remove(itemId)
        return removeResult != null
    }

    override val collectionVariable = "r"

    override val collectionVariablePrefix = "r."

    override val openBracketChar = '['

    override val closeBracketChar = ']'

    override val collectionNameForQuery = "${couchbaseScope.bucketName()}.${couchbaseScope.name()}.`$collectionName`"

    override val collectionElementForQuery = { name: String -> name }




}