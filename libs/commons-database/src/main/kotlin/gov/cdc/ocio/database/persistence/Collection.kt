package gov.cdc.ocio.database.persistence


/**
 * Interface for NoSQL collection methods
 */
interface Collection {

    /**
     * Get a specific item by its ID.
     *
     * @param id String
     * @param classType Class<T>?
     * @return T?
     */
    fun <T> getItem(
        id: String,
        classType: Class<T>?
    ): T?

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

    // The collection variable is the container/collection name referenced when making queries.  For example, with
    // cosmosdb this is the container reference name.  However, with dynamodb this will be empty since dynamodb
    // doesn't use this.
    val collectionVariable: String

    // The collection variable prefix is used to proceed clauses in queries.  For example, with cosmosdb this will
    // be the referenced collection variable name followed by a dot.
    val collectionVariablePrefix: String

    // The collection name used for queries.  For example, with cosmosdb this will simply be the collection name.
    // With dynamodb this will be the table name surrounded by quotes.
    val collectionNameForQuery: String

    val openBracketChar
        get() = '('

    val closeBracketChar
        get() = ')'

    // The collection element for query is a function that transforms the element name used for the query.  For
    // example, with cosmosdb it's a straight pass-through.  For dynamodb, the element name will be surrounded by
    // quotation marks.
    val collectionElementForQuery: (String) -> String

    val timeConversionForQuery: (Long) -> String
        get() = { timeEpoch: Long -> timeEpoch.toString() }
}
