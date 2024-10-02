package gov.cdc.ocio.database.dynamo

import com.google.gson.Gson
import gov.cdc.ocio.database.persistence.Collection
import gov.cdc.ocio.database.models.Report
import gov.cdc.ocio.database.models.ReportDeadLetter
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.protocols.jsoncore.JsonNode


/**
 * DynamoDB repository implementation
 *
 * @property ddbClient DynamoDbClient
 * @property ddbEnhancedClient DynamoDbEnhancedClient
 * @property reportsTableName String
 * @property reportsDeadLetterTableName String
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 * @constructor
 */
class DynamoRepository(tablePrefix: String): ProcessingStatusRepository() {

    private val ddbClient = getDynamoDbClient()

    private val ddbEnhancedClient = getDynamoDbEnhancedClient()

    private val reportsTableName = "$tablePrefix-reports".lowercase()

    private val reportsDeadLetterTableName = "$tablePrefix-reports-deadletter".lowercase()

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

    /**
     * Dynamodb implementation of converting the content map to a JsonNode.
     *
     * @param content Map<*, *>
     * @return Any?
     */
    override fun contentTransformer(content: Map<*, *>): Any? {
        return runCatching {
            val json = Gson().toJson(content, MutableMap::class.java).toString()
            JsonNode.parser().parse(json)
        }.getOrNull()
    }

    /**
     * Obtain a dynamodb client using the environment variable credentials provider.
     *
     * @return DynamoDbClient
     */
    private fun getDynamoDbClient() : DynamoDbClient {
        // Load credentials from the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN environment variables.
        return DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()
    }

    /**
     * Obtain an "enhanced" dynamodb client from the standard dynamodb client.
     *
     * @return DynamoDbEnhancedClient
     */
    private fun getDynamoDbEnhancedClient(): DynamoDbEnhancedClient {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(ddbClient)
            .build()
    }
}