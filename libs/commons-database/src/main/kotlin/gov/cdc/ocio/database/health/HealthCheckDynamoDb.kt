package gov.cdc.ocio.database.health

import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import software.amazon.awssdk.services.dynamodb.DynamoDbClient


/**
 * Concrete implementation of the dynamodb health check.
 */
class HealthCheckDynamoDb: HealthCheckSystem("Dynamo DB") {

    // Lazily initialized DynamoDB client
    private val dynamoDbClient: DynamoDbClient by lazy {
        DynamoDbClient.builder().build()
    }
    /**
     * Perform the dynamodb health check operations.
     */
    override fun doHealthCheck() {
        val result = runCatching {
            dynamoDbClient.listTables()
        }
        if (result.isSuccess)
            status = HealthStatusType.STATUS_UP
        else
            healthIssues = result.exceptionOrNull()?.message
    }


}