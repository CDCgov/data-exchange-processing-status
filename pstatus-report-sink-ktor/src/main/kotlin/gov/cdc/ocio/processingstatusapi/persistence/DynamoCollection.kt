package gov.cdc.ocio.processingstatusapi.persistence

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema


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
    private val enhancedClient: DynamoDbEnhancedClient,
    private val dynamoTableName: String,
    private val tableClassType: Class<R>
): Collection {

    private val dynamoTable: DynamoDbTable<R>? = collectionForClass(tableClassType)

    private fun <R> collectionForClass(classType: Class<R>): DynamoDbTable<R>? {
//        return runCatching {
//            return enhancedClient.table(
//                dynamoTableName,
//                TableSchema.fromBean(classType)
//            )
//        }.getOrNull()

        try {
            val table = enhancedClient.table(
                dynamoTableName,
                TableSchema.fromClass(classType)
            )
            return table
        } catch (e: Exception) {
            println("exception: $e")
        }
        return null
    }

    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
//        TODO("Not yet implemented")
        return listOf()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> createItem(id: String, item: T, classType: Class<T>, partitionKey: String?): Boolean {
//        return runCatching {
//            if (!tableClassType.isAssignableFrom(classType))
//                throw Exception("Dynamo table class type ${tableClassType.name} does not match the item class type ${classType.name}")
//            dynamoTable?.putItem(item as R)
//        }.isSuccess
        val result = runCatching {
            if (!tableClassType.isAssignableFrom(classType))
                throw Exception("Dynamo table class type ${tableClassType.name} does not match the item class type ${classType.name}")
            dynamoTable?.putItem(item as R)
        }
        val exception = result.exceptionOrNull()
        if (exception != null) {
            throw exception
        }
        return result.isSuccess
    }

    override fun deleteItem(itemId: String?, partitionKey: String?): Any {
        TODO("Not yet implemented")
    }

}