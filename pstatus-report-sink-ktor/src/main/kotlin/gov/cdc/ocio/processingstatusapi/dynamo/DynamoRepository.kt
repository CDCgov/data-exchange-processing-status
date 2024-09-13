package gov.cdc.ocio.processingstatusapi.dynamo

import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.ReportDeadLetter
import gov.cdc.ocio.processingstatusapi.persistence.Collection
import gov.cdc.ocio.processingstatusapi.persistence.DynamoCollection
import gov.cdc.ocio.processingstatusapi.persistence.ProcessingStatusRepository
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient


/**
 * DynamoDB repository implementation
 *
 * @property ddbEnhancedClient DynamoDbEnhancedClient
 * @property reportsTableName String
 * @property reportsDeadLetterTableName String
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 * @constructor
 */
class DynamoRepository(dbPrefix: String): ProcessingStatusRepository() {

    private val ddbClient = getDynamoDbClient()

    private val ddbEnhancedClient = getDynamoDbEnhancedClient()

    private val reportsTableName = "$dbPrefix-reports".lowercase()

    private val reportsDeadLetterTableName = "$dbPrefix-reports-deadletter".lowercase()

    override var reportsCollection =
        DynamoCollection(
            ddbClient,
            ddbEnhancedClient,
            reportsTableName,
            Report::class.java
        ) as Collection

    override var reportsDeadLetterCollection = DynamoCollection(
        ddbClient,
        ddbEnhancedClient,
        reportsDeadLetterTableName,
        ReportDeadLetter::class.java
    ) as Collection

    private fun getDynamoDbClient() : DynamoDbClient {
        // Load credentials from the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN environment variables.
        return DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()
    }

    private fun getDynamoDbEnhancedClient(): DynamoDbEnhancedClient {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(ddbClient)
            .build()
    }
}