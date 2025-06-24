package gov.cdc.ocio.database.persistence

import io.opentelemetry.api.GlobalOpenTelemetry
import kotlin.time.measureTimedValue

/**
 * A class that provides telemetry functionality for actions performed on a NoSQL collection.
 * It extends the Collection interface and acts as a decorator to measure performance metrics
 * such as latency for retrieving items or querying items in the underlying collection.
 *
 * @constructor Creates an instance of CollectionDataFetcher with a delegate Collection.
 * @param delegate The underlying collection implementation that this class wraps.
 */
class CollectionDataFetcher(
    private val delegate: Collection
) : Collection {

    private val meter = GlobalOpenTelemetry.get().getMeter(delegate.collectionNameForQuery)

    /**
     * A histogram instrument used to record the latency of get item requests.
     *
     * The histogram measures the time taken, in milliseconds, to retrieve an item
     * from the database. It is configured as a long-value histogram for precise
     * measurement of time durations. This instrument is primarily utilized to
     * collect performance metrics for monitoring and analysis purposes.
     */
    private val getItemLatencyHistogram = meter
        .histogramBuilder("get_item_request_latency")
        .setDescription("Time taken to get the item from the database")
        .setUnit("ms")
        .ofLongs()
        .build()

    /**
     * A histogram metric used to record the latency, in milliseconds, of
     * querying items from the database. This metric tracks the time it takes to
     * fulfill a query request by measuring the elapsed duration for each query operation.
     */
    private val queryItemsLatencyHistogram = meter
        .histogramBuilder("query_items_request_latency")
        .setDescription("Time taken to get the query items from the database")
        .setUnit("ms")
        .ofLongs()
        .build()

    /**
     * A histogram metric for measuring the latency of item creation requests.
     *
     * This metric tracks the time, in milliseconds, taken to create an item in the database.
     */
    private val createItemLatencyHistogram = meter
        .histogramBuilder("create_item_request_latency")
        .setDescription("Time taken to create an item in the database")
        .setUnit("ms")
        .ofLongs()
        .build()

    /**
     * Histogram metric for tracking the latency of delete item requests.
     *
     * The metric records the time taken to delete an item from the database,
     * measured in milliseconds.
     */
    private val deleteItemLatencyHistogram = meter
        .histogramBuilder("delete_item_request_latency")
        .setDescription("Time taken to delete an item in the database")
        .setUnit("ms")
        .ofLongs()
        .build()

    /**
     * Retrieves a specific item identified by its ID and type.
     *
     * @param id the unique identifier of the item to be retrieved
     * @param classType the class type of the item to be retrieved, used for deserialization
     * @return the retrieved item of type T, or null if not found
     */
    override fun <T> getItem(id: String, classType: Class<T>?): T? {
        val (result, duration) = measureTimedValue {
            delegate.getItem(id, classType)
        }
        getItemLatencyHistogram.record(duration.inWholeMilliseconds)
        return result
    }

    /**
     * Queries items in the collection based on the specified query and class type.
     *
     * @param query The query string used to filter items in the collection.
     * @param classType The class type of the items to be retrieved, used for deserialization.
     * @return A list of items of type T that match the query criteria.
     */
    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        val (result, duration) = measureTimedValue {
            delegate.queryItems(query, classType)
        }
        queryItemsLatencyHistogram.record(duration.inWholeMilliseconds)
        return result
    }

    /**
     * Creates a new item in the collection.
     *
     * @param id the unique identifier of the item to be created
     * @param item the item to be created
     * @param classType the class type of the item, used for serialization
     * @param partitionKey the partition key for the item, or null if not applicable
     * @return true if the item is successfully created, false otherwise
     */
    override fun <T> createItem(id: String, item: T, classType: Class<T>, partitionKey: String?): Boolean {
        val (result, duration) = measureTimedValue {
            delegate.createItem(id, item, classType, partitionKey)
        }
        createItemLatencyHistogram.record(duration.inWholeMilliseconds)
        return result
    }

    /**
     * Deletes an item from the collection based on its identifier and partition key.
     *
     * @param itemId the unique identifier of the item to be deleted
     * @param partitionKey the partition key associated with the item, or null if not applicable
     * @return true if the item was successfully deleted, false otherwise
     */
    override fun deleteItem(itemId: String?, partitionKey: String?): Boolean {
        val (result, duration) = measureTimedValue {
            delegate.deleteItem(itemId, partitionKey)
        }
        deleteItemLatencyHistogram.record(duration.inWholeMilliseconds)
        return result
    }

    override val collectionVariable = delegate.collectionVariable

    override val collectionVariablePrefix = delegate.collectionVariablePrefix

    override val collectionNameForQuery = delegate.collectionNameForQuery

    override val collectionElementForQuery = delegate.collectionElementForQuery
}