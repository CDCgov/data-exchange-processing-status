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
class HealthCheckDynamoDb: HealthCheckSystem("Dynamo DB") {

    private val awsCredentialProvider: WebIdentityTokenFileCredentialsProvider by lazy {
        WebIdentityTokenFileCredentialsProvider.builder()
        .roleArn(System.getenv("AWS_ROLE_ARN"))
        .webIdentityTokenFile(Path.of(System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE")))
        .build()
    }

    // Lazily initialized DynamoDB client
    private val dynamoDbClient: DynamoDbClient by lazy {
        DynamoDbClient.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(awsCredentialProvider)
        .build()
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