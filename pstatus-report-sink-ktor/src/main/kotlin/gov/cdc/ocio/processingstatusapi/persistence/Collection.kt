package gov.cdc.ocio.processingstatusapi.persistence

import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.google.gson.Gson
import org.bson.Document


interface Collection {

    fun <T> queryItems(
        query: String?,
        classType: Class<T>?
    ): List<T>

    fun <T> createItem(
        item: T,
        partitionKey: String?
    ): Boolean
}

class CosmosCollection(private val cosmosContainer: CosmosContainer?): Collection {

    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        val items = cosmosContainer?.queryItems(
            query, CosmosQueryRequestOptions(),
            classType
        )

        return items?.toList() ?: listOf()
    }

    override fun <T> createItem(item: T, partitionKey: String?): Boolean {
        val response = cosmosContainer?.createItem(
            item,
            PartitionKey(partitionKey),
            CosmosItemRequestOptions()
        )

        return when (response?.statusCode) {
            in 200..201 -> true
            else -> false
        }
    }

}

class MongoCollection(private val mongoCollection: com.mongodb.client.MongoCollection<Document>): Collection {

    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        TODO("Not yet implemented")
    }

    override fun <T> createItem(item: T, partitionKey: String?): Boolean {
        val document = Document.parse(Gson().toJson(item))
        val result = mongoCollection.insertOne(document)
        return result.wasAcknowledged()
    }

}
