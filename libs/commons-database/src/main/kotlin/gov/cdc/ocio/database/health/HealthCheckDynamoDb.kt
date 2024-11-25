package gov.cdc.ocio.database.health

import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider


/**
 * Concrete implementation of the dynamodb health check.
 */
class HealthCheckDynamoDb: HealthCheckSystem("Dynamo DB") {

    private val awsCredentialProvider: WebIdentityTokenFileCredentialsProvider by lazy {
        WebIdentityTokenFileCredentialsProvider.build()
        .roleArn(System.getenv("AWS_ROLE_ARN))
        .webIdentityTokenFile(System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE"))
    }

    // Lazily initialized DynamoDB client
    private val dynamoDbClient: DynamoDbClient by lazy {
        DynamoDbClient.builder()
        .region(Region.US_EAST_1)
        .build()
        .credentialsProvider(awsCredentialProvider)

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