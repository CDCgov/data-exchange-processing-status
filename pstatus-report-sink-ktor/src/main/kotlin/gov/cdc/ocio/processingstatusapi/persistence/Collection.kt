package gov.cdc.ocio.processingstatusapi.persistence


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
     * @param item T
     * @param partitionKey String?
     * @return Boolean
     */
    fun <T> createItem(
        item: T,
        partitionKey: String?
    ): Boolean
}
