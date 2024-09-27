package gov.cdc.ocio.processingstatusapi.persistence

import mu.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.*
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementRequest


/**
 * DynamoDB collection implementation
 *
 * @property enhancedClient DynamoDbEnhancedClient
 * @property dynamoTableName String
 * @property tableClassType Class<R>
 * @property dynamoTable DynamoDbTable<R>?
 * @constructor
 */
class DynamoCollection<R>(
    private val dynamoDbClient: DynamoDbClient,
    private val enhancedClient: DynamoDbEnhancedClient,
    private val dynamoTableName: String,
    private val tableClassType: Class<R>
) : Collection {

    private val logger = KotlinLogging.logger {}

    private val dynamoTable: DynamoDbTable<R>? = collectionForClass(tableClassType)

    private fun <R> collectionForClass(classType: Class<R>): DynamoDbTable<R>? {
        try {
            val table = enhancedClient.table(
                dynamoTableName,
                TableSchema.fromClass(classType)
            )
            return table
        } catch (e: Exception) {
            logger.error("Unable to establish the dynamodb enhanced client for table: ${classType.name} with exception: $e")
        }
        return null
    }

    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        return try {
            val request = ExecuteStatementRequest.builder()
                .statement(query)
                .build()
            val response = dynamoDbClient.executeStatement(request)
            val tableSchema = TableSchema.fromClass(classType)
            val reports = response.items().map { tableSchema.mapToItem(it) }
            reports
        } catch (exception: Exception) {
            logger.error("Exception executing query: $exception")
            listOf()
        }
    }

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

    override fun deleteItem(itemId: String?, partitionKey: String?): Boolean {
        val key = Key.builder()
            .partitionValue(itemId)
            .build()

        val result = dynamoTable?.deleteItem(key)

        return result != null
    }

}