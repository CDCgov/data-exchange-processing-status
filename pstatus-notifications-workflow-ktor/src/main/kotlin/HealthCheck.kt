package gov.cdc.ocio.processingnotifications

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.config.TemporalConfig
import gov.cdc.ocio.processingnotifications.temporal.HealthCheckTemporalServer
import gov.cdc.ocio.types.health.HealthCheck
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthStatusType
import gov.cdc.ocio.types.utils.TimeUtils


/**
 * Service for querying the health of the temporal server and its dependencies.
 *
 * @property logger KLogger
 * @property repository ProcessingStatusRepository
 * @property temporalConfig TemporalConfig
 * @property temporalHealthCheckServer HealthCheckTemporalServer
 */
class HealthCheckService : KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val temporalConfig: TemporalConfig by inject()

    private val temporalHealthCheckServer = HealthCheckTemporalServer(temporalConfig)

    /**
     * Returns a HealthCheck object with the overall health of temporal server and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {

        val temporalHealthCheck: HealthCheckResult
        var databaseHealthCheck: HealthCheckResult

        val time = measureTimeMillis {
            databaseHealthCheck = repository.healthCheckSystem.doHealthCheck()
            temporalHealthCheck = temporalHealthCheckServer.doHealthCheck()
        }

        return HealthCheck().apply {
            status = if (databaseHealthCheck.status == HealthStatusType.STATUS_UP
                && temporalHealthCheck.status == HealthStatusType.STATUS_UP
            )
                HealthStatusType.STATUS_UP
            else
                HealthStatusType.STATUS_DOWN

            totalChecksDuration = TimeUtils.formatMillisToHMS(time)
            dependencyHealthChecks.add(databaseHealthCheck)
            dependencyHealthChecks.add(temporalHealthCheck)
        }
    }
}
