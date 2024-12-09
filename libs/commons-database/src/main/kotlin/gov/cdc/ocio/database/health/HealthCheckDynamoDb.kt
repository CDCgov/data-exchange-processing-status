package gov.cdc.ocio.database.health

import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider
import java.nio.file.Path


/**
 * Concrete implementation of the dynamodb health check.
 */
class HealthCheckDynamoDb(
    private val dynamoDbClient: DynamoDbClient? = null // Injected client - optional
) : HealthCheckSystem("Dynamo DB") {


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
     */
    override fun doHealthCheck() {
        val client = dynamoDbClient ?: defaultDynamoDbClient // Use provided or default client
        val result = runCatching {
            client.listTables()
        }
        if (result.isSuccess)
            status = HealthStatusType.STATUS_UP
        else
            healthIssues = result.exceptionOrNull()?.message
    }


}