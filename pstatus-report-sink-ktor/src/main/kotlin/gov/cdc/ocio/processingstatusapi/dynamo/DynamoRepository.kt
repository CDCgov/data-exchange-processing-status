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
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse


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

    private val ddbEnhancedClient = getDynamoDbEnhancedClient()

    private val reportsTableName = "$dbPrefix-reports".lowercase()

    private val reportsDeadLetterTableName = "$dbPrefix-reports-deadletter".lowercase()

    override var reportsCollection = DynamoCollection(ddbEnhancedClient, reportsTableName, Report::class.java) as Collection

    override var reportsDeadLetterCollection = DynamoCollection(ddbEnhancedClient, reportsDeadLetterTableName, ReportDeadLetter::class.java) as Collection

    private fun getDynamoDbEnhancedClient(): DynamoDbEnhancedClient {

        // Load credentials from the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN environment variables.
        val ddb = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()

        listAllTables(ddb)

        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(ddb)
            .build()
    }

    fun listAllTables(ddb: DynamoDbClient) {
        var moreTables = true
        var lastName: String? = null

        while (moreTables) {
            try {
                var response: ListTablesResponse? = null
                if (lastName == null) {
                    val request = ListTablesRequest.builder().build()
                    response = ddb.listTables(request)
                } else {
                    val request = ListTablesRequest.builder()
                        .exclusiveStartTableName(lastName).build()
                    response = ddb.listTables(request)
                }

                val tableNames = response.tableNames()
                if (tableNames.size > 0) {
                    for (curName in tableNames) {
                        System.out.format("* %s\n", curName)
                    }
                } else {
                    println("No tables found!")
                    System.exit(0)
                }

                lastName = response.lastEvaluatedTableName()
                if (lastName == null) {
                    moreTables = false
                }
            } catch (e: DynamoDbException) {
                System.err.println(e.message)
                System.exit(1)
            }
        }
        println("\nDone!")
    }

}