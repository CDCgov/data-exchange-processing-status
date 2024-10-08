package gov.cdc.ocio.processingstatusapi.health

import gov.cdc.ocio.database.DatabaseType
import gov.cdc.ocio.processingstatusapi.MessageSystem
import gov.cdc.ocio.processingstatusapi.health.database.*
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckAWSSQS
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckRabbitMQ
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckServiceBus
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckUnsupportedMessageSystem
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

    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        val databaseHealthCheck: HealthCheckSystem?
        val messageSystemHealthCheck: HealthCheckSystem?

        val time = measureTimeMillis {
            databaseHealthCheck = when (databaseType) {
                DatabaseType.COSMOS -> HealthCheckCosmosDb()
                DatabaseType.MONGO -> HealthCheckMongoDb()
                DatabaseType.COUCHBASE -> HealthCheckCouchbaseDb()
                DatabaseType.DYNAMO -> HealthCheckDynamoDb()
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
        }
        return compileHealthChecks(databaseHealthCheck, messageSystemHealthCheck, time)
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
        totalTime: Long
    ): HealthCheck {

        return HealthCheck().apply {
            status = if (databaseHealth?.status == HealthCheck.STATUS_UP
                && messageSystemHealth?.status == HealthCheck.STATUS_UP
            )
                HealthCheck.STATUS_UP else HealthCheck.STATUS_DOWN

            totalChecksDuration = formatMillisToHMS(totalTime)
            databaseHealth?.let { dependencyHealthChecks.add(it) }
            messageSystemHealth?.let { dependencyHealthChecks.add(it) }
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