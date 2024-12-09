package gov.cdc.ocio.database.dynamo

import gov.cdc.ocio.database.persistence.Collection
import mu.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.*
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementRequest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


/**
 * DynamoDB collection implementation
 *
 * @property enhancedClient DynamoDbEnhancedClient
 * @property dynamoTableName String
 * @property tableClassType Class<R>
 * @property dynamoTable DynamoDbTable<R>?
 * @constructor Creates a DynamoDB collection for use with the [Collection] interface.
 *
 * @see [DynamoRepository]
 * @see [Collection]
 */
class DynamoCollection<R>(
    private val dynamoDbClient: DynamoDbClient,
    private val enhancedClient: DynamoDbEnhancedClient,
    private val dynamoTableName: String,
    private val tableClassType: Class<R>
) : Collection {

    private val logger = KotlinLogging.logger {}

    private val dynamoTable = collectionForClass(tableClassType)

    /**
     * Builds a dynamodb table schema from the provided class type.
     *
     * @param classType Class<R>
     * @return DynamoDbTable<R>?
     */
    private fun <R> collectionForClass(classType: Class<R>): DynamoDbTable<R>? {
        return try {
            enhancedClient.table(
                dynamoTableName,
                TableSchema.fromClass(classType)
            )
        } catch (e: Exception) {
            logger.error("Unable to establish the dynamodb enhanced client for table: ${classType.name} with exception: $e")
            null
        }
    }

    /**
     * Execute the provided query and return the results as POJOs.
     *
     * @param query String?
     * @param classType Class<T>?
     * @return List<T>
     */
    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        return try {
            val request = ExecuteStatementRequest.builder()
                .statement(query)
                .build()
            val response = dynamoDbClient.executeStatement(request)
            // Build a table schema for the provided class type
            val tableSchema = TableSchema.fromClass(classType)
            // Map each resultant from the query response to POJO items
            val items = response.items().map { tableSchema.mapToItem(it) }
            items
        } catch (exception: Exception) {
            logger.error("Exception executing query: $exception")
            listOf()
        }
    }

    /**
     * Create a dynamodb item from the provided data.
     *
     * @param id String
     * @param item T
     * @param classType Class<T>
     * @param partitionKey String?
     * @return Boolean
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> createItem(id: String, item: T, classType: Class<T>, partitionKey: String?): Boolean {
        val result = runCatching {
            if (!tableClassType.isAssignableFrom(classType))
                throw Exception("Dynamo table class type ${tableClassType.name} does not match the item class type ${classType.name}")
            dynamoTable?.putItem(item as R)
        }
        val exception = result.exceptionOrNull()
        if (exception != null) {
            logger.error("Exception creating an item: $exception")
            throw exception
        }
        return result.isSuccess
    }

    /**
     * Delete the specified item id from dynamodb.
     *
     * @param itemId String?
     * @param partitionKey String?
     * @return Boolean
     */
    override fun deleteItem(itemId: String?, partitionKey: String?): Boolean {
        val key = Key.builder()
            .partitionValue(itemId)
            .build()

        val result = dynamoTable?.deleteItem(key)

        // We know the deletion was successful if the result is non-null
        return result != null
    }

    // dynamodb does not use variables (implicit)
    override val collectionVariable = ""

    // dynamodb does not use variables (implicit)
    override val collectionVariablePrefix = ""

    // The table name must be surrounded by quotes for dynamodb.
    override val collectionNameForQuery = "\"$dynamoTableName\""

    // Elements "sometimes" need to be surrounded by quotes for dynamodb, so do it always.
    override val collectionElementForQuery = { name: String -> "\"$name\"" }

    override val isArrayNotEmptyOrNull = "SIZE"
}