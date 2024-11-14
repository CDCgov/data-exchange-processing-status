package gov.cdc.ocio.database.dynamo

import com.google.gson.Gson
import gov.cdc.ocio.database.persistence.Collection
import gov.cdc.ocio.database.models.Report
import gov.cdc.ocio.database.models.ReportDeadLetter
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.protocols.jsoncore.JsonNode


/**
 * DynamoDB implementation of the processing status repository.
 *
 * @param tablePrefix[String] The table prefix is prepended to each of the table names for the reports and
 * deadlettered reports table names.
 * @property ddbClient [DynamoDbClient]
 * @property ddbEnhancedClient [DynamoDbEnhancedClient]
 * @property reportsTableName [String]
 * @property reportsDeadLetterTableName [String]
 * @property notificationSubscriptionsTableName [String]
 * @property reportsCollection [Collection]
 * @property reportsDeadLetterCollection [Collection]
 * @property notificationSubscriptionsCollection [Collection]
 * @constructor Provides a DynamoDB repository, which is a concrete implementation of the [ProcessingStatusRepository]
 *
 * @see [ProcessingStatusRepository]
 */
class DynamoRepository(tablePrefix: String): ProcessingStatusRepository() {

    private val ddbClient = getDynamoDbClient()

    private val ddbEnhancedClient = getDynamoDbEnhancedClient()

    private val reportsTableName = "$tablePrefix-reports".lowercase()

    private val reportsDeadLetterTableName = "$tablePrefix-reports-deadletter".lowercase()

    private val notificationSubscriptionsTableName = "$tablePrefix-notification-subscriptions".lowercase()

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

    override var notificationSubscriptionsCollection = DynamoCollection(
        ddbClient,
        ddbEnhancedClient,
        notificationSubscriptionsTableName,
        Any::class.java // TODO(This needs to be replaced!)
    ) as Collection

    override val supportsGroupBy = false

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