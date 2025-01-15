package gov.cdc.ocio.processingstatusapi.health

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.reportschemavalidator.loaders.SchemaLoader
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

    private val repository by inject<ProcessingStatusRepository>()

//    private val messageSystem by inject<>()

    private val schemaLoader by inject<SchemaLoader>()

    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        val databaseHealthCheck = repository.healthCheckSystem
        val messageSystemHealthCheck = schemaLoader.healthCheckSystem
        val schemaLoaderSystemHealthCheck = schemaLoader.healthCheckSystem

        val time = measureTimeMillis {
            databaseHealthCheck.doHealthCheck()

            // selectively check health of the messaging service based on msgType
//            messageSystemHealthCheck = when (msgType) {
//                MessageSystem.AZURE_SERVICE_BUS.toString() -> HealthCheckServiceBus()
//                MessageSystem.RABBITMQ.toString() -> HealthCheckRabbitMQ()
//                MessageSystem.AWS.toString() -> HealthCheckAWSSQS()
//                else -> HealthCheckUnsupportedMessageSystem()
//            }
//            messageSystemHealthCheck.doHealthCheck()

            schemaLoaderSystemHealthCheck.doHealthCheck()
        }
        return compileHealthChecks(databaseHealthCheck, messageSystemHealthCheck, schemaLoaderSystemHealthCheck, time)
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