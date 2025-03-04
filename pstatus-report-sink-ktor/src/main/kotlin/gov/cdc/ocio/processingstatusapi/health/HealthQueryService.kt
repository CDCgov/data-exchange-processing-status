package gov.cdc.ocio.processingstatusapi.health

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.reportschemavalidator.loaders.SchemaLoader
import gov.cdc.ocio.types.health.HealthCheck
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthStatusType
import gov.cdc.ocio.types.utils.TimeUtils
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis


/**
 * Service for querying the health of the report-sink service and its dependencies.
 *
 * @property logger KLogger
 * @property repository ProcessingStatusRepository
 * @property messageSystem MessageSystem
 * @property schemaLoader SchemaLoader
 */
class HealthQueryService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val messageSystem by inject<MessageSystem>()

    private val schemaLoader by inject<SchemaLoader>()

    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        var databaseHealthCheck: HealthCheckResult
        var messageSystemHealthCheck: HealthCheckResult
        var schemaLoaderSystemHealthCheck: HealthCheckResult

        val time = measureTimeMillis {
            databaseHealthCheck = repository.healthCheckSystem.doHealthCheck()
            messageSystemHealthCheck = messageSystem.healthCheckSystem.doHealthCheck()
            schemaLoaderSystemHealthCheck = schemaLoader.healthCheckSystem.doHealthCheck()
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
        databaseHealth: HealthCheckResult,
        messageSystemHealth: HealthCheckResult,
        schemaLoaderSystemHealth: HealthCheckResult,
        totalTime: Long
    ): HealthCheck {

        return HealthCheck().apply {
            status = if (databaseHealth.status == HealthStatusType.STATUS_UP
                && messageSystemHealth.status == HealthStatusType.STATUS_UP
                && schemaLoaderSystemHealth.status == HealthStatusType.STATUS_UP
            )
                HealthStatusType.STATUS_UP else HealthStatusType.STATUS_DOWN

            totalChecksDuration = TimeUtils.formatMillisToHMS(totalTime)

            dependencyHealthChecks.add(databaseHealth)
            dependencyHealthChecks.add(messageSystemHealth)
            dependencyHealthChecks.add(schemaLoaderSystemHealth)
        }
    }

}