package gov.cdc.ocio.processingstatusapi.cosmos

import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import gov.cdc.ocio.processingstatusapi.persistence.Collection


/**
 * Cosmos Collection implementation
 *
 * @property cosmosContainer CosmosContainer?
 * @constructor
 */
class CosmosCollection(
    containerName: String,
    private val cosmosContainer: CosmosContainer?
) : Collection {

    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        val items = cosmosContainer?.queryItems(
            query, CosmosQueryRequestOptions(),
            classType
        )

        return items?.toList() ?: listOf()
    }

    override fun <T> createItem(id: String, item: T, classType: Class<T>, partitionKey: String?): Boolean {
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

    override fun deleteItem(itemId: String?, partitionKey: String?): Boolean {
        val response = cosmosContainer?.deleteItem(
            itemId,
            PartitionKey(partitionKey),
            CosmosItemRequestOptions()
        )
        return response != null
    }

    override val collectionVariable = "r"

    override val collectionVariablePrefix = "r."

    override val collectionNameForQuery = containerName

    override val collectionElementForQuery = { name: String -> name }

}