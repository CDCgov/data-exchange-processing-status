package gov.cdc.ocio.database.persistence


/**
 * Interface for NoSQL collection methods
 */
interface Collection {

    /**
     * Query for items in the collection
     *
     * @param query String?
     * @param classType Class<T>?
     * @return List<T>
     */
    fun <T> queryItems(
        query: String?,
        classType: Class<T>?
    ): List<T>

    /**
     * Create an item in the collection
     *
     * @param id String
     * @param item T
     * @param classType Class<T>
     * @param partitionKey String?
     * @return Boolean
     */
    fun <T> createItem(
        id: String,
        item: T,
        classType: Class<T>,
        partitionKey: String?
    ): Boolean

    /**
     * Delete an item in the collection
     *
     * @param itemId String?
     * @param partitionKey String?
     * @return Boolean - true if successful, false otherwise
     */
    fun deleteItem(
        itemId: String?,
        partitionKey: String?
    ): Boolean

    val collectionVariable: String

    val collectionVariablePrefix: String

    val collectionNameForQuery: String

    val collectionElementForQuery: (String) -> String
}
