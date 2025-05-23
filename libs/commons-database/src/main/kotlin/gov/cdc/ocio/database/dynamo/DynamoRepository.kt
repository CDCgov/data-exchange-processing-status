package gov.cdc.ocio.database.dynamo

import com.google.gson.Gson
import gov.cdc.ocio.database.health.HealthCheckDynamoDb
import gov.cdc.ocio.database.persistence.Collection
import gov.cdc.ocio.database.models.Report
import gov.cdc.ocio.database.models.ReportDeadLetter
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.types.health.HealthCheckSystem
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.protocols.jsoncore.JsonNode
import java.nio.file.Path


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
class DynamoRepository(
    tablePrefix: String,
    roleArn:String?,
    webIdentityTokenFile:String?
): ProcessingStatusRepository() {

    private val ddbClient = getDynamoDbClient(roleArn, webIdentityTokenFile)

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
     * Obtain a dynamodb client using the environment variable credentials' provider.
     *
     * @return DynamoDbClient
     */
    private fun getDynamoDbClient(roleArn:String?, webIdentityTokenFile:String?) : DynamoDbClient {

        val credentialsProvider =    if (roleArn.isNullOrEmpty() ||
            webIdentityTokenFile.isNullOrEmpty()) {
            // Fallback to default credentials provider (access key and secret)
            DefaultCredentialsProvider.create()
        } else {
            // Use Web Identity Token
            WebIdentityTokenFileCredentialsProvider.builder()
                .roleArn(roleArn)
                .webIdentityTokenFile(webIdentityTokenFile.let { Path.of(it) })
                .build()
        }
        // Load credentials from the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN environment variables.
        return DynamoDbClient.builder()
            .credentialsProvider(credentialsProvider)
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

    override var healthCheckSystem = HealthCheckDynamoDb(system) as HealthCheckSystem
}