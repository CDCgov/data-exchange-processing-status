package gov.cdc.ocio.database.health

import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse
import java.nio.file.Path


/**
 * Concrete implementation of the dynamodb health check.
 */
class HealthCheckDynamoDb(
    system: String,
    private val dynamoDbClient: DynamoDbClient? = null // Injected client - optional
) : HealthCheckSystem(system, "Dynamo DB") {

    // Lazily initialize the client if none is provided
    private val defaultDynamoDbClient: DynamoDbClient by lazy {
        DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(
                WebIdentityTokenFileCredentialsProvider.builder()
                    .roleArn(System.getenv("AWS_ROLE_ARN"))
                    .webIdentityTokenFile(Path.of(System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE")))
                    .build()
            )
            .build()
    }

    /**
     * Perform the dynamodb health check operations.
     *
     * @return HealthCheckResult
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = isDynamoDbHealthy()
        result.onFailure { error ->
            val reason = "DynamoDB is not accessible and hence not healthy ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(system, service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(system, service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether DynamoDB is accessible.
     *
     * @return Result<Boolean>
     */
    private fun isDynamoDbHealthy(): Result<ListTablesResponse> {
        val client = dynamoDbClient ?: defaultDynamoDbClient // Use provided or default client
        val result = runCatching {
            client.listTables()
        }
        return result
    }

}