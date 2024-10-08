package gov.cdc.ocio.database.cosmos

import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import gov.cdc.ocio.database.persistence.Collection
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KotlinLogging


/**
 * Cosmos Collection implementation.
 *
 * @param containerName[String] Name of the container used for this collection.
 * @property cosmosContainer[CosmosContainer] Cosmos container associated with this collection.
 * @constructor Creates a couchbase collection for use with the [Collection] interface.
 *
 * @see [CosmosRepository]
 * @see [Collection]
 */
class CosmosCollection(
    containerName: String,
    private val cosmosContainer: CosmosContainer?
) : Collection {

    private val logger = KotlinLogging.logger {}

    /**
     * Execute the provided query and return the results as POJOs.
     *
     * @param query String?
     * @param classType Class<T>?
     * @return List<T>
     */
    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        val items = cosmosContainer?.queryItems(
            query, CosmosQueryRequestOptions(),
            classType
        )

        return items?.toList() ?: listOf()
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
        var attempts = 0
        do {
            try {
                val response = cosmosContainer?.createItem(
                    item,
                    PartitionKey(partitionKey),
                    CosmosItemRequestOptions()
                )

                val isValidResponse = response != null
                val statusCode = response?.statusCode

                logger.info("Creating item, response http status code = ${statusCode}, attempt = ${attempts + 1}, id = $id")
                if (isValidResponse) {
                    when (statusCode) {
                        HttpResponseStatus.OK.code(), HttpResponseStatus.CREATED.code() -> {
                            logger.info("Successfully created item with id = $id")
                            return true
                        }

                        HttpResponseStatus.TOO_MANY_REQUESTS.code() -> {
                            // See: https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/performance-tips?tabs=trace-net-core#429
                            // https://learn.microsoft.com/en-us/rest/api/cosmos-db/common-cosmosdb-rest-response-headers
                            // https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/troubleshoot-request-rate-too-large?tabs=resource-specific
                            val recommendedDuration = response.responseHeaders?.get("x-ms-retry-after-ms")
                            logger.warn("Received 429 (too many requests) from cosmosdb, attempt ${attempts + 1}, will retry after $recommendedDuration millis, id = $id")
                            val waitMillis = recommendedDuration?.toLong()
                            Thread.sleep(waitMillis ?: DEFAULT_RETRY_INTERVAL_MILLIS)
                        }

                        else -> {
                            // Need to retry regardless
                            val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                            logger.warn("Received response code ${statusCode}, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis, id = $id")
                            Thread.sleep(retryAfterDurationMillis)
                        }
                    }
                } else {
                    val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                    logger.warn("Received null response from cosmosdb, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis, id = $id")
                    Thread.sleep(retryAfterDurationMillis)
                }
            } catch (e: Exception) {
                val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                logger.error("CreateReport: Exception: ${e.localizedMessage}, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis, id = $id")
                Thread.sleep(retryAfterDurationMillis)
            }

        } while (attempts++ < MAX_RETRY_ATTEMPTS)

        return false
    }

    /**
     * Delete the specified item from the container.
     *
     * @param itemId String?
     * @param partitionKey String?
     * @return Boolean
     */
    override fun deleteItem(itemId: String?, partitionKey: String?): Boolean {
        val response = cosmosContainer?.deleteItem(
            itemId,
            PartitionKey(partitionKey),
            CosmosItemRequestOptions()
        )
        return response != null
    }

    /**
     * The function which calculates the interval after which the retry should occur
     *
     * @param attempt Int
     * @return Long
     */
    private fun getCalculatedRetryDuration(attempt: Int) = DEFAULT_RETRY_INTERVAL_MILLIS * (attempt + 1)

    override val collectionVariable = "r"

    override val collectionVariablePrefix = "r."

    override val collectionNameForQuery = containerName

    override val collectionElementForQuery = { name: String -> name }

    companion object {
        const val DEFAULT_RETRY_INTERVAL_MILLIS = 500L
        const val MAX_RETRY_ATTEMPTS = 100
    }
}