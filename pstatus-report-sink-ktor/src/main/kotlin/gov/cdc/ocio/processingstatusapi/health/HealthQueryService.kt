package gov.cdc.ocio.processingstatusapi.health

import gov.cdc.ocio.database.DatabaseType
import gov.cdc.ocio.database.health.*
import gov.cdc.ocio.processingstatusapi.MessageSystem
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckAWSSQS
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckRabbitMQ
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckServiceBus
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckUnsupportedMessageSystem
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckBlobContainer
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckFileSystem
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckS3Bucket
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckUnsupportedSchemaLoaderSystem
import gov.cdc.ocio.reportschemavalidator.utils.SchemaLoaderSystemType
import gov.cdc.ocio.types.health.HealthCheck
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis


/**
 * Service for querying the health of the report-sink service and its dependencies.
 *
 * @property logger KLogger
 * @property msgType String
 */
class HealthQueryService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val databaseType: DatabaseType by inject()

    private val msgType: String by inject()

    private val schemaLoaderSystemType:SchemaLoaderSystemType by inject()

    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        val databaseHealthCheck: HealthCheckSystem?
        val messageSystemHealthCheck: HealthCheckSystem?
        val schemaLoaderSystemHealthCheck: HealthCheckSystem?

        val time = measureTimeMillis {
            databaseHealthCheck = when (databaseType) {
                DatabaseType.COSMOS -> getKoin().get<HealthCheckCosmosDb>()
                DatabaseType.MONGO -> getKoin().get<HealthCheckMongoDb>()
                DatabaseType.COUCHBASE -> getKoin().get<HealthCheckCouchbaseDb>()
                DatabaseType.DYNAMO -> getKoin().get<HealthCheckDynamoDb>()
                else -> HealthCheckUnsupportedDb()
            }
            databaseHealthCheck.doHealthCheck()

            // selectively check health of the messaging service based on msgType
            messageSystemHealthCheck = when (msgType) {
                MessageSystem.AZURE_SERVICE_BUS.toString() -> HealthCheckServiceBus()
                MessageSystem.RABBITMQ.toString() -> HealthCheckRabbitMQ()
                MessageSystem.AWS.toString() -> HealthCheckAWSSQS()
                else -> HealthCheckUnsupportedMessageSystem()
            }
            messageSystemHealthCheck.doHealthCheck()

            schemaLoaderSystemHealthCheck = when (schemaLoaderSystemType.toString().lowercase()) {
                SchemaLoaderSystemType.S3.toString().lowercase() ->  getKoin().get<HealthCheckS3Bucket>()
                SchemaLoaderSystemType.BLOB_STORAGE.toString().lowercase() ->  getKoin().get<HealthCheckBlobContainer>()
                SchemaLoaderSystemType.FILE_SYSTEM.toString().lowercase() -> HealthCheckFileSystem()
                else -> HealthCheckUnsupportedSchemaLoaderSystem()
            }
            schemaLoaderSystemHealthCheck.doHealthCheck()
        }
        return compileHealthChecks(databaseHealthCheck, messageSystemHealthCheck,schemaLoaderSystemHealthCheck, time)
    }

    /**
     * Compiles health checks for supported services
     *
     * @param databaseHealth HealthCheckSystem? Health check for the database
     * @param messageSystemHealth HealthCheckSystem? Health check for the messaging system
     * @param totalTime Long
     * @return HealthCheck
     */
    private fun compileHealthChecks(
        databaseHealth: HealthCheckSystem?,
        messageSystemHealth: HealthCheckSystem?,
        schemaLoaderSystemHealth:HealthCheckSystem?,
        totalTime: Long
    ): HealthCheck {

        return HealthCheck().apply {
            status = if (databaseHealth?.status == HealthStatusType.STATUS_UP
                && messageSystemHealth?.status == HealthStatusType.STATUS_UP
                && schemaLoaderSystemHealth?.status == HealthStatusType.STATUS_UP
            )
                HealthStatusType.STATUS_UP else HealthStatusType.STATUS_DOWN

            totalChecksDuration = formatMillisToHMS(totalTime)

            databaseHealth?.let { dependencyHealthChecks.add(it) }
            messageSystemHealth?.let { dependencyHealthChecks.add(it) }
            schemaLoaderSystemHealth?.let { dependencyHealthChecks.add(it) }
        }
    }

    /**
     * Format the time in milliseconds to 00:00:00.000 format.
     *
     * @param millis Long
     * @return String
     */
    private fun formatMillisToHMS(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        val remainingMillis = millis % 1000

        return "%02d:%02d:%02d.%03d".format(hours, minutes, remainingSeconds, remainingMillis / 10)
    }
}