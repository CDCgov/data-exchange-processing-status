package gov.cdc.ocio.database.health

import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import software.amazon.awssdk.services.dynamodb.DynamoDbClient


/**
 * Concrete implementation of the dynamodb health check.
 */
class HealthCheckDynamoDb: HealthCheckSystem("Dynamo DB") {

    /**
     * Perform the dynamodb health check operations.
     */
    override fun doHealthCheck() {
        val result = runCatching {
            getDynamoDbClient().listTables()
        }
        if (result.isSuccess)
            status = HealthStatusType.STATUS_UP
        else
            healthIssues = result.exceptionOrNull()?.message
    }

    /**
     * Return a dynamodb client from the environment variables.
     *
     * @return DynamoDbClient
     */
    private fun getDynamoDbClient() : DynamoDbClient {
        // Load credentials from the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN environment variables.
        return DynamoDbClient.builder()
            .build()
    }
}